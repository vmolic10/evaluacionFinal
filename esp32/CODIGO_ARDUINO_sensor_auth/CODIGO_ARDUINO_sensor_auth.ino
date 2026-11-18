// Verificador de funcionamiento del sensor HC-SR04 con ESP32 + Firebase (versión segura)
//
// - Mide la distancia y detecta cuándo el sensor "actúa" (objeto cerca).
// - Revisa que el sensor funcione bien (si no recibe eco, informa FALLA).
// - Se autentica en Firebase con su propia cuenta (correo y contraseña), igual que los usuarios
// de la app, y envía el estado a Firebase. Las reglas de la base de datos solo dejan entrar
// a usuarios autenticados.
// - Lee el interruptor ON/OFF que manda la app.
//
// No necesita librerías externas.
//
// MODO_WOKWI = 1 -> simulador Wokwi (WiFi "Wokwi-GUEST")
// MODO_WOKWI = 0 -> placa real (pon tu WiFi abajo)
//
// IMPORTANTE: no subas a GitHub tu WiFi, la clave de API ni la contraseña del ESP32.
// Deja los textos de ejemplo en el archivo que subas.
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>
#include <time.h>

// ================== CONFIGURACIÓN ==================
#define MODO_WOKWI 1

#if MODO_WOKWI
const char* WIFI_SSID = "Wokwi-GUEST";
const char* WIFI_PASS = "";
const int WIFI_CANAL = 6;
#else
const char* WIFI_SSID = "TU_WIFI";
const char* WIFI_PASS = "TU_CLAVE";
const int WIFI_CANAL = 0;
#endif

const char* DB_URL = "https://appfirebase-e1224-default-rtdb.firebaseio.com";

// Cuenta del ESP32 en Firebase Authentication (se crea en la consola de Firebase)
const char* API_KEY = "AIzaSyCrJrDIOBM7P_ag0b6MRb6zPWjK10jdk4k";           
const char* FIREBASE_USER = "esp32@proyecto.com";
const char* FIREBASE_PASS = "Vichomolina.10";

// Pines
const int PIN_TRIG = 5;
const int PIN_ECHO = 18;  // en la placa real, con divisor de voltaje (1k + 2k)
const int LED_VERDE = 26; // sensor funcionando (parpadea si hay falla)
const int LED_ROJO = 33;  // objeto detectado

// Ajustes
const float UMBRAL_DETECCION_CM = 30.0;  // menos que esto = "el sensor está actuando"
const int FALLAS_PARA_ERROR = 3;         // lecturas sin eco seguidas para declarar FALLA
const unsigned long MEDIR_CADA_MS = 500;
const unsigned long ENVIAR_CADA_MS = 2000;
const unsigned long TOKEN_VIDA_MS = 3000000UL;  // el token dura 1 h; se renueva a los 50 min
// ===================================================

WiFiClientSecure cliente;
HTTPClient http;

String idToken = "";               // credencial temporal que entrega Firebase Authentication
unsigned long tokenObtenidoMs = 0;

bool sistemaActivo = true;     // lo controla la app
float distancia = -1;
bool detectando = false;
bool funcionando = false;
int fallasSeguidas = 0;
int detecciones = 0;
long ultimaDeteccion = 0;
const char* estado = "iniciando";  // iniciando | ok | detectando | falla | apagado

unsigned long ultimaMedicion = 0;
unsigned long ultimoEnvio = 0;
unsigned long ultimoIntentoWifi = 0;
bool peticionOk = false;

// ---------- Utilidades ----------
long ahora() {  // hora UTC en segundos (0 si todavía no se sincroniza)
  time_t t = time(nullptr);
  return (t > 1700000000) ? (long)t : 0;
}

// Saca el valor de un campo de texto de una respuesta JSON simple
String extraerCampo(const String& json, const char* campo) {
  String clave = String("\"") + campo + "\"";
  int i = json.indexOf(clave);
  if (i < 0) return "";
  i = json.indexOf(':', i);
  if (i < 0) return "";
  int ini = json.indexOf('"', i + 1);
  if (ini < 0) return "";
  int fin = json.indexOf('"', ini + 1);
  if (fin < 0) return "";
  return json.substring(ini + 1, fin);
}

// ---------- Firebase Authentication ----------
// Inicia sesión con correo y contraseña y guarda el token. Devuelve true si funcionó.
bool iniciarSesionFirebase() {
  if (WiFi.status() != WL_CONNECTED) return false;

  WiFiClientSecure clienteAuth;  // conexión aparte: es otro servidor
  clienteAuth.setInsecure();
  HTTPClient httpAuth;
  String url = String("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=") + API_KEY;
  httpAuth.begin(clienteAuth, url);
  httpAuth.setTimeout(8000);
  httpAuth.addHeader("Content-Type", "application/json");

  String cuerpo = String("{\"email\":\"") + FIREBASE_USER + "\",\"password\":\"" + FIREBASE_PASS +
                  "\",\"returnSecureToken\":true}";
  int codigo = httpAuth.POST(cuerpo);
  String respuesta = (codigo > 0) ? httpAuth.getString() : "";
  httpAuth.end();

  if (codigo == 200) {
    idToken = extraerCampo(respuesta, "idToken");
    if (idToken.length() > 0) {
      tokenObtenidoMs = millis();
      Serial.println("Sesión en Firebase iniciada");
      return true;
    }
  }
  Serial.printf("No se pudo iniciar sesión en Firebase (HTTP %d). Revisa API_KEY, correo y clave del ESP32.\n", codigo);
  idToken = "";
  return false;
}

// Petición REST a Firebase Realtime Database, con el token de autenticación
String peticion(const char* metodo, const String& ruta, const String& cuerpo = "") {
  peticionOk = false;
  if (idToken.length() == 0) return "";

  String url = String(DB_URL) + ruta + ".json?auth=" + idToken;
  http.begin(cliente, url);
  http.setTimeout(5000);
  http.addHeader("Content-Type", "application/json");
  int codigo = http.sendRequest(metodo, cuerpo);
  String respuesta = "";
  peticionOk = (codigo >= 200 && codigo < 300);
  if (peticionOk) {
    respuesta = http.getString();
  } else {
    Serial.printf("Error HTTP %s %s -> %d\n", metodo, ruta.c_str(), codigo);
    if (codigo == 401) idToken = "";  // token vencido o rechazado: se vuelve a iniciar sesión
  }
  http.end();
  return respuesta;
}

// Guarda un aviso en el historial de la app (nodo /Acciones)
void registrarEvento(const String& texto) {
  Serial.println("EVENTO: " + texto);
  if (WiFi.status() != WL_CONNECTED) return;
  String json = "{\"rut\":\"ESP32\",\"nombre\":\"Sensor HC-SR04\",\"accion\":\"" + texto +
                "\",\"fecha\":{\".sv\":\"timestamp\"}}";
  peticion("POST", "/Acciones", json);
}

// ---------- Sensor ----------
float medirUnaVez() {
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(3);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);

  unsigned long duracion = pulseIn(PIN_ECHO, HIGH, 30000);  // máx. 30 ms
  if (duracion == 0) return -1;                             // sin eco
  float cm = duracion * 0.034 / 2;
  if (cm < 2.0 || cm > 400.0) return -1;                    // fuera de rango
  return cm;
}

float medirDistancia() {  // mediana de 3 lecturas
  float v[3];
  int n = 0;
  for (int i = 0; i < 3; i++) {
    float d = medirUnaVez();
    if (d > 0) v[n++] = d;
    delay(25);
  }
  if (n == 0) return -1;
  for (int i = 1; i < n; i++) {
    float x = v[i];
    int j = i - 1;
    while (j >= 0 && v[j] > x) { v[j + 1] = v[j]; j--; }
    v[j + 1] = x;
  }
  return v[n / 2];
}

// Mide y decide en qué estado está el sensor
void medirYEvaluar() {
  if (!sistemaActivo) {
    estado = "apagado";
    detectando = false;
    funcionando = false;
    fallasSeguidas = 0;
    return;
  }

  const char* estadoAntes = estado;
  bool detectandoAntes = detectando;
  float d = medirDistancia();

  if (d < 0) {  // no hubo eco
    fallasSeguidas++;
    if (fallasSeguidas >= FALLAS_PARA_ERROR) {
      funcionando = false;
      detectando = false;
      estado = "falla";
      if (strcmp(estadoAntes, "falla") != 0) {
        registrarEvento("FALLA del sensor: no recibe eco, revisa el cableado");
      }
    }
    return;
  }

  // Lectura válida: el sensor funciona
  distancia = d;
  fallasSeguidas = 0;
  funcionando = true;
  detectando = (d < UMBRAL_DETECCION_CM);
  estado = detectando ? "detectando" : "ok";

  if (strcmp(estadoAntes, "falla") == 0) {
    registrarEvento("Sensor recuperado: vuelve a medir correctamente");
  }
  if (detectando && !detectandoAntes) {
    detecciones++;
    ultimaDeteccion = ahora();
    registrarEvento("Objeto detectado a " + String(d, 0) +
                    " cm: el sensor está actuando correctamente");
  }

  Serial.printf("Distancia: %.1f cm | estado: %s | detecciones: %d\n", d, estado, detecciones);
}

void actualizarLeds() {
  if (strcmp(estado, "falla") == 0) {
    digitalWrite(LED_VERDE, (millis() / 300) % 2);  // parpadea = falla
  } else {
    digitalWrite(LED_VERDE, funcionando ? HIGH : LOW);
  }
  digitalWrite(LED_ROJO, detectando ? HIGH : LOW);
}

// ---------- Firebase ----------
void cicloRed() {
  if (WiFi.status() != WL_CONNECTED) return;

  // 0. Sesión: si no hay token o está por vencer, se inicia sesión de nuevo
  if (idToken.length() == 0 || millis() - tokenObtenidoMs > TOKEN_VIDA_MS) {
    if (!iniciarSesionFirebase()) return;
  }

  // 1. ¿La app encendió o apagó el sistema?
  String r = peticion("GET", "/Datos/activo");
  if (peticionOk) {
    r.trim();
    if (r == "false") sistemaActivo = false;
    else if (r == "true") sistemaActivo = true;
    else if (r == "null") peticion("PUT", "/Datos/activo", "true");  // primera vez: se crea
  }

  // 2. Enviar el estado del sensor (una sola petición)
  String json = "{";
  json += "\"estado\":\"" + String(estado) + "\"";
  json += ",\"detectando\":" + String(detectando ? "true" : "false");
  json += ",\"funcionando\":" + String(funcionando ? "true" : "false");
  json += ",\"detecciones\":" + String(detecciones);
  if (distancia > 0) json += ",\"distancia\":" + String(distancia, 1);
  if (ultimaDeteccion > 0) json += ",\"ultimaDeteccion\":" + String(ultimaDeteccion);
  long t = ahora();
  if (t > 0) json += ",\"ultimaLectura\":" + String(t);
  json += "}";
  peticion("PATCH", "/Datos", json);
}

void mantenerWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  if (millis() - ultimoIntentoWifi > 5000) {
    ultimoIntentoWifi = millis();
    Serial.println("WiFi caído, reintentando...");
    WiFi.disconnect();
    WiFi.begin(WIFI_SSID, WIFI_PASS, WIFI_CANAL);
  }
}

// ---------- Arduino ----------
void setup() {
  Serial.begin(115200);
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  pinMode(LED_VERDE, OUTPUT);
  pinMode(LED_ROJO, OUTPUT);

  Serial.print("Conectando a WiFi");
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS, WIFI_CANAL);
  unsigned long inicio = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - inicio < 20000) {
    Serial.print(".");
    delay(500);
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi conectado!");
  } else {
    Serial.println("\nSin WiFi por ahora; se seguirá intentando.");
  }

  configTime(0, 0, "pool.ntp.org", "time.google.com");
  cliente.setInsecure();   // conexión cifrada (HTTPS) sin validar el certificado del servidor
  http.setReuse(true);     // mantiene la conexión abierta: más rápido
}

void loop() {
  if (millis() - ultimaMedicion >= MEDIR_CADA_MS) {
    ultimaMedicion = millis();
    medirYEvaluar();
  }
  actualizarLeds();

  if (millis() - ultimoEnvio >= ENVIAR_CADA_MS) {
    ultimoEnvio = millis();
    cicloRed();
  }
  mantenerWiFi();
}
