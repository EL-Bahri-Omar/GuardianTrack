# Rapport Technique — GuardianTrack
## Application de Sécurité Personnelle

**Date :** Avril 2026  
**Plateforme :** Android (API 26+)  
**Langage :** Kotlin  
**Architecture :** MVVM + Clean Architecture  
**UI :** Jetpack Compose + Material 3  

---

## Table des Matières

1. [Introduction et Contexte](#1-introduction-et-contexte)
2. [Questions Obligatoires](#2-questions-obligatoires)
3. [Architecture et Structure du Projet](#3-architecture-et-structure-du-projet)
4. [Base de Données Room — Description des Tables](#4-base-de-données-room)
5. [Grille d'Évaluation](#5-grille-dévaluation)
6. [Conclusion](#6-conclusion)

---

## 1. Introduction et Contexte

GuardianTrack est une application Android de sécurité personnelle développée en Kotlin. Elle surveille l'accéléromètre du téléphone en temps réel via un **Foreground Service** pour détecter les chutes. Lorsqu'une chute est détectée, l'application :

- **Enregistre** l'incident (type, horodatage, coordonnées GPS) dans une base Room
- **Affiche** une notification d'urgence sur le téléphone
- **Envoie un SMS réel** à tous les contacts d'urgence configurés
- **Synchronise** les incidents vers une API distante via WorkManager

L'application supporte le mode clair/sombre, fonctionne en mode offline-first, et est entièrement construite avec Jetpack Compose et Material 3.

---

## 2. Questions Obligatoires

### Question 1 — Pourquoi Flow plutôt que LiveData pour la communication entre Room et le ViewModel ?

Dans GuardianTrack, nous avons choisi `Flow` pour plusieurs raisons spécifiques à notre architecture :

**1. Compatibilité avec les coroutines :** Notre `SurveillanceService` utilise un `CoroutineScope(SupervisorJob() + Dispatchers.Default)` pour gérer les événements de chute. Flow s'intègre naturellement dans cet écosystème, tandis que LiveData aurait nécessité des conversions supplémentaires (`asFlow()`/`asLiveData()`).

**2. Opérations en chaîne :** Dans `UserPreferencesRepository`, nous chaînons des transformations :

```kotlin
val userPreferences: Flow<UserPreferencesData> = dataStore.data.map { preferences ->
    UserPreferencesData(
        sensitivityThreshold = preferences[SENSITIVITY_THRESHOLD] ?: 15.0f,
        isDarkMode = preferences[DARK_MODE] ?: false,
        // ...
    )
}
```

Flow permet ces transformations fonctionnelles (`map`, `filter`, `combine`) de manière plus élégante que `LiveData.Transformations`.

**3. Thread-safety :** Le `SurveillanceService` collecte le seuil de sensibilité sur `Dispatchers.Default` :

```kotlin
serviceScope.launch {
    userPreferencesRepository.sensitivityThreshold.collect { threshold ->
        fallDetector.updateThreshold(threshold)
    }
}
```

Flow fonctionne sur n'importe quel dispatcher, tandis que LiveData est limité au main thread.

**4. Cold Stream :** Room retourne un Flow froid — les données ne sont émises que quand un collecteur est actif. C'est optimal pour `HistoryScreen` qui ne reçoit des mises à jour que quand elle est visible.

**Quand aurions-nous choisi LiveData ?** Si le projet n'utilisait pas Compose (qui préfère Flow via `collectAsStateWithLifecycle()`), ou si nous utilisions des Fragments avec ViewBinding classique, LiveData aurait été plus approprié car il gère automatiquement le cycle de vie.

---

### Question 2 — Gestion du refus définitif de ACCESS_FINE_LOCATION

Dans GuardianTrack, la localisation est **critique mais non bloquante**. Notre stratégie de repli est implémentée dans `LocationHelper.getCurrentLocation()` :

**Vérification :**
```kotlin
if (checkSelfPermission(ACCESS_FINE_LOCATION) != GRANTED &&
    checkSelfPermission(ACCESS_COARSE_LOCATION) != GRANTED) {
    return Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)  // 0.0, 0.0
}
```

**Stratégie de repli :**
- L'incident est **quand même enregistré** dans Room avec `latitude=0.0, longitude=0.0`
- La détection de chute (accéléromètre) fonctionne **indépendamment** de la localisation
- Le SMS est envoyé avec la mention *"Location unavailable"*

**Cas `shouldShowRequestPermissionRationale = false` :** L'utilisateur a coché "Ne plus demander". Nous pourrions afficher un dialogue guidant vers les paramètres système :

```kotlin
Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.fromParts("package", packageName, null)
}
```

**Pourquoi cette approche ?** Dans un contexte de sécurité personnelle, il est préférable d'enregistrer un incident **sans position** plutôt que de ne pas l'enregistrer du tout.

---

### Question 3 — Limites de sécurité du ContentProvider et protection contre les injections

**Protection implémentée :** Notre `EmergencyContactProvider` est protégé par :

```xml
<permission
    android:name="com.guardian.track.permission.READ_EMERGENCY_CONTACTS"
    android:protectionLevel="signature|privileged" />
```

Seules les applications signées avec la **même clé** peuvent accéder aux données.

**Limites :**
- Les apps système (privileged) peuvent contourner la protection
- Sur un appareil rooté, toute protection peut être contournée
- Si la clé de signature est compromise, la protection tombe

**Attaque de type Content Provider Injection :**  
Un attaquant manipule les paramètres SQL dans `query()`/`update()` :

```kotlin
// Vulnérable (ne PAS faire) :
cursor = query(uri, null, "name = " + userInput, null, null)
// userInput = "'; DROP TABLE contacts; --"
```

**Notre protection :**
1. Paramètres de sélection paramétrés (`selectionArgs`) — pas de concaténation
2. URI validée via `UriMatcher` avant tout accès
3. Permission signature empêche toute app tierce d'appeler le provider
4. Données exposées en lecture seule pour les apps clientes

---

### Question 4 — Restrictions services arrière-plan Android 12+ et ACTION_BOOT_COMPLETED

**Restrictions Android 12 (API 31) :** Les applications ne peuvent plus démarrer un Foreground Service depuis un contexte en arrière-plan. `startForegroundService()` depuis un `BroadcastReceiver` lève une `ForegroundServiceStartNotAllowedException`.

**Notre solution — WorkManager comme intermédiaire :**

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "service_start",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ServiceStartWorker>().build()
            )
        }
    }
}
```

Le `ServiceStartWorker` est exécuté par le système dans un contexte autorisé, ce qui lui permet d'appeler `startForegroundService()` sans violer les restrictions.

**Pourquoi c'est propre :**
- Respecte les restrictions du système Android 12+
- WorkManager garantit l'exécution même si le système est sous pression
- `ExistingWorkPolicy.KEEP` évite les duplications

---

### Question 5 — Séparation Entity / DTO / DomainModel

Chaque couche a des contraintes différentes :

| Couche | Rôle | Contrainte |
|--------|------|------------|
| **Entity** (Room) | Persistance dans SQLite | Annotée `@Entity`, IDs auto-générés, types SQLite |
| **DTO** (Réseau) | Communication avec l'API | Format dicté par le serveur (ISO8601, objets imbriqués) |
| **Domain Model** | Logique métier | Aucune dépendance technique, types Kotlin natifs |

**Exemple concret dans GuardianTrack :**

```kotlin
// Entity Room — liée à SQLite
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,           // Unix timestamp
    val type: String,              // "FALL" (texte brut)
    val latitude: Double,
    @ColumnInfo(name = "is_synced")
    val isSynced: Int              // 0 ou 1 (SQLite n'a pas de Boolean)
)

// Domain Model — propre et typé
data class Incident(
    val id: Long,
    val type: IncidentType,        // Enum typé (FALL, BATTERY, MANUAL)
    val isSynced: Boolean,         // Boolean natif Kotlin
    val formattedDateTime: String  // Propriété calculée pour l'UI
)
```

**Valeur ajoutée :** Si l'API change son format → seul le DTO change. Si Room ajoute un index → seule l'Entity change. Le ViewModel ne dépend jamais de Room ni de Retrofit.

---

### Question 6 — WorkManager vs JobScheduler vs AlarmManager

| Critère | WorkManager | JobScheduler | AlarmManager |
|---------|-------------|-------------|--------------|
| Contraintes réseau | ✅ Natif | ✅ Manuel | ❌ Non |
| Survie redémarrage | ✅ Automatique | ❌ Manuel | ❌ Non |
| Retry automatique | ✅ Backoff | ❌ Manuel | ❌ Non |
| Chaînage de tâches | ✅ Natif | ❌ Non | ❌ Non |
| API minimum | 14 | 21 | 1 |

**Justification pour GuardianTrack :**

1. **Contrainte réseau :** La synchronisation ne doit s'exécuter que quand le réseau est disponible :

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()
```

2. **Offline-first :** Les incidents sont stockés localement dans Room. WorkManager planifie la synchronisation pour plus tard, même hors-ligne.

3. **Survie au redémarrage :** Si le téléphone redémarre avant la synchro, WorkManager réexécute automatiquement le Worker.

4. **Retry avec backoff :** En cas d'échec réseau, WorkManager replanifie avec un backoff exponentiel, sans code supplémentaire.

---

## 3. Architecture et Structure du Projet

```
┌─────────────────────────────────────────────────────────────┐
│                  UI LAYER (Jetpack Compose)                  │
│  DashboardScreen → DashboardViewModel                        │
│  HistoryScreen   → HistoryViewModel                          │
│  SettingsScreen  → SettingsViewModel                         │
├─────────────────────────────────────────────────────────────┤
│                     DOMAIN LAYER                             │
│  Incident, EmergencyContact, IncidentType                    │
├─────────────────────────────────────────────────────────────┤
│                      DATA LAYER                              │
│  Room (incidents, contacts) | DataStore | Retrofit           │
│  IncidentRepository | ContactRepository                      │
├─────────────────────────────────────────────────────────────┤
│                 SERVICES & WORKERS                            │
│  SurveillanceService | FallDetector | SyncWorker             │
│  BootReceiver | BatteryReceiver | ContentProvider            │
├─────────────────────────────────────────────────────────────┤
│                   INJECTION (Hilt)                            │
│  AppModule | DatabaseModule | NetworkModule                  │
└─────────────────────────────────────────────────────────────┘
```

**Algorithme de détection de chute (deux phases) :**
1. **Free-fall :** magnitude < 3.0 m/s² (proche de l'apesanteur)
2. **Impact :** magnitude > seuil configurable, dans les 500ms

`magnitude = √(ax² + ay² + az²)` calculée à partir de l'accéléromètre.

---

## 4. Base de Données Room

### Table `incidents`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER | Clé primaire auto-incrémentée |
| `timestamp` | INTEGER | Timestamp Unix (ms depuis epoch) |
| `type` | TEXT | "FALL", "BATTERY", ou "MANUAL" |
| `latitude` | REAL | Latitude GPS au moment de l'incident |
| `longitude` | REAL | Longitude GPS au moment de l'incident |
| `is_synced` | INTEGER | 0 = non synchronisé, 1 = envoyé à l'API |

### Table `emergency_contacts`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | INTEGER | Clé primaire auto-incrémentée |
| `name` | TEXT | Nom du contact d'urgence |
| `phone_number` | TEXT | Numéro de téléphone pour SMS |

### Table `room_master_table` (système)

Table interne de Room contenant le hash du schéma. Utilisée pour détecter les migrations de base de données.

---

## 5. Grille d'Évaluation

| Critère | Points | Statut |
|---------|--------|--------|
| Architecture MVVM + Hilt | /20 | ✅ 3 modules Hilt, survie rotation |
| Foreground Service + Détection | /20 | ✅ Free-fall + Impact, configurable |
| Room + DataStore + Export | /15 | ✅ Entités, DAO, export CSV |
| Retrofit + Sealed Classes + WorkManager | /10 | ✅ Offline-first, sync différée |
| BroadcastReceiver (BOOT + BATTERY) | /10 | ✅ Compatible Android 12+ |
| ContentProvider sécurisé | /10 | ✅ Permission signature |
| Permissions Dynamiques + UX | /10 | ✅ Rationale, mode simulation |
| Rapport Technique | /5 | ✅ Ce document |
| **TOTAL** | **/100** | |
| *BONUS — Jetpack Compose* | *+5* | ✅ UI 100% Compose + Material 3 |
| *BONUS — Algorithme amélioré* | *+3* | ✅ Two-phase detection |
| **TOTAL POSSIBLE** | **/108** | |

---

## 6. Conclusion

GuardianTrack démontre l'utilisation des **4 piliers Android** (Activity, Service, BroadcastReceiver, ContentProvider) dans un contexte réel de sécurité personnelle. L'architecture MVVM avec Hilt assure une séparation claire des responsabilités, tandis que l'approche offline-first (Room + WorkManager) garantit la fiabilité même sans connexion réseau.

Le choix de Jetpack Compose pour l'UI permet une interface futuriste et réactive avec support complet du mode clair/sombre et des animations Material 3.
