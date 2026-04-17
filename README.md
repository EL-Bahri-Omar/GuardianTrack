# GuardianTrack — Application Android de Sécurité Personnelle

## 📋 Description

GuardianTrack est une application Android de sécurité personnelle qui surveille en temps réel l'état de l'appareil, détecte les chutes via l'accéléromètre, stocke les incidents localement et les synchronise avec un serveur distant.

## 🏗️ Architecture

### Pattern MVVM strict

```
View (Compose Screens)     →  Affichage uniquement, observe StateFlow
    ↕
ViewModel (@HiltViewModel) →  Logique de présentation, survit aux rotations
    ↕
Repository (@Singleton)    →  Source de vérité unique (Room + Retrofit + WorkManager)
    ↕
Model (Entity / DTO / Domain) → Données pures, séparation stricte
```

### Les 4 Piliers Android

| Pilier | Implémentation |
|--------|---------------|
| **Activity** | `MainActivity` unique + 3 écrans Compose (Dashboard, History, Settings) via Navigation Compose |
| **Service** | `SurveillanceService` — Foreground Service avec `foregroundServiceType="location"` |
| **BroadcastReceiver** | `BatteryLowReceiver` (statique, ACTION_BATTERY_LOW) + `BootCompletedReceiver` (statique, BOOT_COMPLETED) |
| **ContentProvider** | `EmergencyContactProvider` — URI contractuelle `content://com.guardian.track.provider/emergency_contacts` |

### Jetpack Compose (Bonus +5 pts)

L'application utilise **Jetpack Compose** avec Material Design 3 en remplacement complet de ViewBinding/XML. Les "Fragments" du cahier des charges sont implémentés comme des écrans Compose avec leurs propres ViewModels.

## 🔐 Sécurité

### ContentProvider — Protection par Permission Personnalisée

```xml
<permission
    android:name="com.guardian.track.READ_EMERGENCY_CONTACTS"
    android:protectionLevel="signature|privileged" />
```

**Justification du niveau `signature|privileged`** :
- `signature` : Seules les applications signées avec le même certificat que GuardianTrack peuvent accéder aux contacts d'urgence. Cela empêche toute application tierce d'accéder aux données.
- `privileged` : Permet également aux applications système préinstallées d'accéder aux données si nécessaire (ex: application d'urgence du fabricant).
- Ce niveau est préféré à `normal` (trop permissif) et `dangerous` (demande une confirmation utilisateur qui n'a pas de sens pour des données inter-applications de ce type).

### EncryptedSharedPreferences

Le numéro d'urgence et la clé API sont chiffrés via `EncryptedSharedPreferences` (Jetpack Security) avec AES256-GCM. Un fallback vers SharedPreferences standard est implémenté en cas d'échec de l'initialisation du chiffrement.

### Clé API — local.properties

**⚠️ Le fichier `local.properties` n'est PAS versionné** (présent dans `.gitignore`).

La clé API / URL de base est injectée au build via `BuildConfig` :

```properties
# local.properties (NE PAS COMMITTER)
GUARDIAN_API_BASE_URL=https://votre-api.mockapi.io/api/v1/
```

### Permissions Dynamiques

| Permission | Quand | Gestion du refus |
|------------|-------|-----------------|
| `ACCESS_FINE_LOCATION` | Au démarrage | Incident enregistré avec lat=0.0/lng=0.0 (valeur sentinelle) |
| `SEND_SMS` | Au démarrage | SMS non envoyé, notification de simulation utilisée |
| `POST_NOTIFICATIONS` (API 33+) | Au démarrage | Notifications silencieusement ignorées |

**Stratégie GPS refusé définitivement** : Si `shouldShowRequestPermissionRationale` retourne `false`, une dialog invite l'utilisateur à activer la permission dans les paramètres système. En attendant, les incidents sont enregistrés avec les coordonnées sentinelles (0.0, 0.0).

## 📡 Synchronisation Offline-First

| Scénario | Comportement |
|----------|-------------|
| Réseau disponible | Envoi immédiat via Retrofit. Si succès : `isSynced = true` |
| Pas de réseau | Stockage Room (`isSynced = false`) + WorkManager planifié avec contrainte `NETWORK_CONNECTED` |
| Retour du réseau | WorkManager reprend automatiquement et synchronise tous les incidents en attente |

Les états réseau sont modélisés avec `sealed class NetworkResult<T>` (Success, Error, Loading).

## 📱 Contraintes Android 12+ (API 31+)

### Démarrage de service depuis BroadcastReceiver

Sur API 31+, les services foreground ne peuvent pas être démarrés directement depuis un BroadcastReceiver. **Solution implémentée** :

```kotlin
// BootCompletedReceiver.kt
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // WorkManager avec setExpedited() comme solution de repli
    ServiceStartWorker.enqueue(context)
} else {
    SurveillanceService.start(context)
}
```

`ServiceStartWorker` utilise `setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` comme exigé par le cahier des charges.

### Foreground Service Type (API 34)

Déclaré dans le Manifest : `android:foregroundServiceType="location"`

### POST_NOTIFICATIONS (API 33)

Permission demandée dynamiquement dans `MainActivity` avant toute notification. Le refus est géré gracieusement (try/catch SecurityException).

## 🔧 Algorithme de Détection de Chute

Algorithme en deux phases temporelles consécutives :

| Phase | Critère | Description |
|-------|---------|-------------|
| 1 — Chute libre | `magnitude < 3 m/s²` pendant `> 100 ms` | L'appareil est en chute libre |
| 2 — Impact | `magnitude > seuil` dans une fenêtre de `200 ms` | L'appareil heurte une surface |

**Formule** : `magnitude = √(ax² + ay² + az²)`

Le seuil d'impact (défaut: 15.0 m/s²) est configurable via le SettingsScreen (range: 3.0 – 30.0 m/s²).

## 🛠️ Configuration

### Prérequis
- Android Studio Ladybug (2024.x) ou plus récent
- Kotlin 2.0.21
- Android SDK API 36

### Installation

1. Cloner le projet
2. Configurer `local.properties` :
   ```properties
   sdk.dir=C\:\\Users\\...\\Android\\Sdk
   GUARDIAN_API_BASE_URL=https://votre-api.mockapi.io/api/v1/
   ```
3. Ouvrir dans Android Studio → Sync Gradle
4. Run sur émulateur ou appareil (API 26+)

### API Backend

Créer un endpoint MockAPI (mockapi.io) avec une ressource `incidents` acceptant :
```json
{
  "timestamp": 1712345678000,
  "type": "FALL",
  "latitude": 48.8566,
  "longitude": 2.3522
}
```

## 📦 Dépendances Principales

| Technologie | Usage |
|-------------|-------|
| Jetpack Compose + Material 3 | Interface utilisateur |
| Hilt | Injection de dépendances |
| Room | Base de données locale |
| Retrofit + OkHttp | Communication réseau |
| WorkManager | Synchronisation différée |
| DataStore Preferences | Préférences utilisateur |
| Jetpack Security (EncryptedSharedPreferences) | Chiffrement des données sensibles |
| Google Play Services Location | Géolocalisation |
| Navigation Compose | Navigation entre écrans |
| Coroutines + Flow | Concurrence asynchrone |

## 📁 Structure du Projet

```
com.guardian.track/
├── GuardianTrackApp.kt          # @HiltAndroidApp
├── MainActivity.kt              # Single Activity
├── data/
│   ├── local/
│   │   ├── db/                  # Room (entities, DAOs, database)
│   │   ├── datastore/           # DataStore Preferences
│   │   └── security/            # EncryptedSharedPreferences
│   ├── remote/
│   │   ├── api/                 # Retrofit (service, DTOs)
│   │   └── NetworkResult.kt     # Sealed class
│   └── repository/              # Repositories (source de vérité)
├── di/                          # Hilt Modules
├── domain/model/                # Domain models
├── service/                     # ForegroundService + FallDetector
├── receiver/                    # BroadcastReceivers
├── provider/                    # ContentProvider
├── worker/                      # WorkManager workers
├── ui/
│   ├── theme/                   # Material 3 theme
│   ├── navigation/              # NavHost + bottom nav
│   ├── dashboard/               # Dashboard screen + VM
│   ├── history/                 # History screen + VM
│   ├── settings/                # Settings screen + VM
│   └── components/              # Composants réutilisables
└── util/                        # Helpers (notification, location, SMS, CSV)
```

## 📄 Licence

Projet académique — Usage éducatif uniquement.
