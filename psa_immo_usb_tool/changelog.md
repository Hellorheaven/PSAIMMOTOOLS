# Changelog — PSA Immo Tool

## ✅ Version figée (stable)

Cette version est marquée comme **immuable**, base de développement propre et fonctionnelle.

---

### ✅ Noyau fonctionnel

- Support complet des modules :
  - CANBUS (USB série)
  - OBD2 USB (FTDI)
  - OBD2 Bluetooth (ELM327)
  - K-Line USB
- Lecture VIN (tous modules), lecture PIN (CAN)
- Écoute temps réel des trames CAN
- Envoi de trames personnalisées CAN & K-Line
- Gestion de logs : export `.txt`, nettoyage, affichage live
- Fichier `LogExporter.kt` ajouté
- UI responsive pour écran portrait/paysage

---

### 🌍 Multilingue complet

- Tous les textes d'interface traduits (FR + EN)
- Messages d’erreurs, logs, boutons, menus, décodage dynamique multilingue
- Menu avec bascule instantanée entre Français 🇫🇷 et Anglais 🇬🇧
- Ressources centralisées dans :
  - `strings.xml`
  - `strings-en.xml`

---

### 🛠️ Optimisations techniques

- Utilisation correcte de `ContextProvider.appContext` dans tous les modules
- Suppression des warnings Kotlin :
  - `Unresolved reference: get`
  - `invoke() is not found`
  - `Variable declaration could be moved into 'when'`
  - `StringFormatInvalid`
- Ajout de `@SuppressLint("StringFormatInvalid")` lorsque pertinent
- Optimisation de la portée des variables locales dans `when`

---

### 🔐 Permissions

- Gestion dynamique :
  - `ACCESS_FINE_LOCATION`
  - `BLUETOOTH_CONNECT`
  - USB permission via `UsbManager`
- Compatibilité assurée entre Android 9 (API 28) et Android 14 (API 34+)

---

### 🧠 Décodage CAN

- Module `FrameDecoder.kt` robuste :
  - Trames VIN (`09 02`)
  - Trames PIN IMMO (`22 F1 90`)
  - PID OBD2 : RPM (`0C`), température moteur (`05`)
  - Trames PSAR (`FD`) :
    - Commandes volant (Volume, Source…)
    - Température de consigne clim
- Structure lisible et traduisible dans tous les cas d’erreur ou valeur

---

## 🧊 Version figée

Cette version est considérée comme **propre, testée, figée**, utilisée comme socle pour toute future extension :
- Ajout de nouveaux modules
- Extension du décodage
- Amélioration graphique

---
