# Water Leakage Detection — Machine Learning System & Android App

A comprehensive Machine Learning system and native Android application designed to detect pipeline water leakages using physical sensor readings (Pressure and Refill status) evaluated by an ensemble of four pre-trained scikit-learn classification models.

---

## 1. Project Overview

The **Water Leakage Detection** project bridges industrial predictive maintenance with mobile software engineering. Leveraging pre-trained models from an industrial multi-tank network benchmark, the solution consists of:
* A **FastAPI Python Backend** serving four trained scikit-learn classification models through an unweighted probability averaging ensemble.
* An **SQLite Database** (`water_leakage.db`) persisting physical readings, GPS coordinates, timestamps, individual model predictions, and ensemble probabilities across restarts.
* A native **Android Application** (Kotlin + Jetpack Compose + Material 3) built with a minimal, modern white and sky blue aesthetic, featuring a real-time Dashboard, Add Reading form, Prediction Results breakdown, Reading History with status filtering, and Reading Details.
* **One-Click Automation Scripts** (`run_backend.bat`, `bridge_usb.bat`) that bridge connections to physical phones over USB and start the server seamlessly.

> [!NOTE]
> This application is built for inspection simulations and manual entry of pipe readings. It does not connect to real-time physical IoT hardware or telemetry sensors.

---

## 2. Problem Statement

Water distribution networks and industrial storage tanks face continuous risks of pressure loss, pipe bursts, and micro-fissures. Inspecting tanks manually is time-consuming and inefficient. By analyzing fluid pressure drops and automated refill cycles, machine learning models can classify whether an observed reading indicates normal pipe operations or an active water leak.

---

## 3. Project Objectives

1. Leverage the benchmark 100-tank industrial water distribution dataset (`IndustrialNetwork/GraphDataset`).
2. Utilize four classification algorithms:
   * **Logistic Regression**
   * **Decision Tree**
   * **Random Forest**
   * **Support Vector Machine (SVM with RBF Kernel)**
3. Deploy an ensemble decision mechanism combining individual model posterior probabilities rather than hard class votes.
4. Deliver an asynchronous REST API with persistent SQLite database storage.
5. Provide a production-grade Android mobile interface for engineers and evaluators.

---

## 4. Machine Learning Approach

The models evaluate two physical features to predict pipeline condition:
1. **Pressure**: Measured fluid pressure. Nominal normal operating levels typically lie in the **45.0 to 65.0** bar range. Readings below 45 (pressure loss) or above 65 (pressure surge) frequently indicate leaks.
2. **Refill**: Binary indicator (`0` or `1`) designating whether an automated pump is refilling the tank.

Metadata such as Pipe ID, GPS coordinates, and inspection timestamps are saved for auditing and record-keeping, but are deliberately **not** fed into the ML models.

### Feature Normalization
Input features are normalized using a pre-fitted `StandardScaler`:
$$\mu = [54.2536, 0.06025], \quad \sigma = [14.4685, 0.2379]$$

---

## 5. Dataset Description

* **Source**: `IndustrialNetwork/GraphDataset` on GitHub.
* **Original Structure**: 12 CSV files (`measurements_1.csv` to `measurements_12.csv`) covering 100,000 timesteps across 100 tanks (301 columns).
* **Flattened Records**: 10,000,000 tank-time observation rows.
* **Class Balance**:
  * No Leak (Class 0): 7,319,875 (73.2%)
  * Leak (Class 1): 2,680,125 (26.8%)
* **Training Sample**: A stratified sample of 20,000 rows partitioned 80/20 into 16,000 training and 4,000 test records.

---

## 6. Four Machine Learning Models Used

| Model | Hyperparameters / Setup | Role & Performance |
| :--- | :--- | :--- |
| **Logistic Regression** | Default (`L2` penalty) | Linear decision boundary; high precision (91.75%) on distinct extremes. |
| **Decision Tree** | `random_state=42` | Non-linear partitioning; high recall (57.93%). |
| **Random Forest** | `n_estimators=100, random_state=42` | Ensemble of 100 bagged trees providing stable probabilities. |
| **SVM** | `SVC(kernel='rbf', probability=True, random_state=42)` | RBF kernel; best overall Accuracy (85.10%) and F1-score (65.98%). |

---

## 7. Probability Averaging Formula and Decision Threshold

Rather than majority voting on binary class labels, the ensemble calculates the **unweighted arithmetic mean of the predicted leak probabilities**:

$$\text{Average Probability} = \frac{P_{\text{LR}}(\text{Leak}) + P_{\text{DT}}(\text{Leak}) + P_{\text{RF}}(\text{Leak}) + P_{\text{SVM}}(\text{Leak})}{4}$$

### Decision Threshold
* **$\text{Average Probability} \ge 0.50$ (50%)** $\longrightarrow$ **Leak**
* **$\text{Average Probability} < 0.50$ (50%)** $\longrightarrow$ **No Leak**

---

## 8. Technology Stack

### Backend
* **Language**: Python 3.11
* **Framework**: FastAPI (Asynchronous REST API)
* **Server**: Uvicorn (ASGI)
* **Database**: SQLite with SQLAlchemy ORM
* **Data Validation**: Pydantic v2
* **Machine Learning**: Scikit-Learn 1.6.1, Joblib, NumPy, Pandas

### Android Application
* **Language**: Kotlin 2.3+
* **UI Framework**: Jetpack Compose (Material 3)
* **Design System**: Minimalist White Primary (`#FFFFFF`) with Sky Blue Accents (`#0284C7`), Slate Typography, and Material Vector Icons
* **Networking**: Retrofit 2, OkHttp 4, Gson
* **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
* **Navigation**: Jetpack Navigation Compose
* **Location APIs**: Android Location Services & Runtime Permissions

---

## 9. Mobile App UI & Design System

The mobile application has been built from the ground up with a clean, minimal design aesthetic:

* **Primary Background**: Pure White (`#FFFFFF`) and Soft Off-White canvas (`#F8FAFC`).
* **Accent Colors**: Sky Blue (`#0284C7` / `#0EA5E9` / `#E0F2FE`).
* **Surfaces**: Crisp white cards with subtle 1.dp borders (`#E2E8F0`).
* **Typography**: Slate 900 (`#0F172A`) for titles, Slate 600 (`#475569`) for subtitles, and Slate 400 (`#94A3B8`) for metadata labels.
* **Status Colors**: Rose Red (`#E11D48`) for Leaks, Mint Green (`#059669`) for Normal operation.
* **Vector Icons**: Exclusively standard Material vector icons (no emojis anywhere in the app).

### Five Complete Screens:
1. **Dashboard**:
   - Header with ensemble title and quick action buttons (Refresh, Settings).
   - Live server status banner displaying active URL, connectivity status dot, and auto-probe action.
   - Four key metric cards: Total Readings, Leaks Detected, Normal Pipes, and Model Count.
   - Quick navigation banner to full Reading History.
   - Recent Predictions card list with tap-to-inspect navigation.
   - Floating Action Button to log a new reading.
2. **Add Reading**:
   - Sensor inputs card: Pipe ID and Pressure (bar) with nominal range hints.
   - Refill status toggle switch.
   - Inspection metadata card: Latitude, Longitude, and ISO 8601 Timestamp.
   - One-tap GPS coordinate acquisition button.
   - Solid sky blue submit button with loading indicator during inference.
3. **Prediction Results**:
   - High-visibility hero status card (Rose Red for Leak, Mint Green for Normal).
   - Ensemble probability gauge indicating distance from the 50% cutoff line.
   - Model breakdown card displaying individual probabilities and predictions for all 4 models.
   - Reading metadata summary card.
   - Quick action buttons (View History and Add Next).
4. **Reading History**:
   - Live search bar filtering by Pipe ID.
   - Three filter chips (All, Leaks Only, Normal Only).
   - Interactive history cards with leak badges and average probabilities.
5. **Reading Details**:
   - Complete record audit with status badge, probability gauge, 4-model evaluation matrix, sensor inputs, and GPS metadata.
   - Delete reading button with confirmation dialog.

---

## 10. Complete Project Folder Structure

```text
WaterLeakageDetection/
├── README.md                           # Comprehensive documentation
├── run_backend.bat                     # One-click startup script (starts server & bridges USB)
├── bridge_usb.bat                      # Standalone USB port bridge script
├── ml.ipynb                            # Reference Google Colab machine learning notebook
│
├── backend/                            # FastAPI Python Backend
│   ├── app/
│   │   ├── config.py                   # App settings, DB path, artifacts dir
│   │   ├── database.py                 # SQLite SQLAlchemy engine & session
│   │   ├── main.py                     # FastAPI app instance, CORS & lifecycle
│   │   ├── models.py                   # Reading SQLAlchemy ORM model
│   │   ├── schemas.py                  # Pydantic v2 request/response schemas
│   │   ├── routers/
│   │   │   ├── dashboard.py            # GET /api/v1/dashboard/summary
│   │   │   └── readings.py             # CRUD & prediction endpoints
│   │   └── services/
│   │       └── ml_service.py           # Preprocessing, inference & ensemble logic
│   ├── artifacts/                      # Pre-trained ML binaries
│   │   ├── scaler.joblib               # Fitted StandardScaler
│   │   ├── logistic_regression.joblib  # Trained Logistic Regression
│   │   ├── decision_tree.joblib        # Trained Decision Tree
│   │   ├── random_forest.joblib        # Trained Random Forest
│   │   └── svm.joblib                  # Trained SVM (probability=True)
│   ├── tests/
│   │   └── test_backend.py             # Pytest test suite (ML + DB + Endpoints)
│   ├── requirements.txt                # Python dependencies (scikit-learn==1.6.1)
│   └── water_leakage.db                # SQLite persistent database file
│
├── android/                            # Native Android Jetpack Compose App
│   ├── app/
│   │   ├── build.gradle.kts            # App-level build config (compileSdk 36, Java 17)
│   │   ├── build/outputs/apk/debug/
│   │   │   └── app-debug.apk           # Pre-compiled, ready-to-install debug APK
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml # Permissions & cleartext network settings
│   │       │   ├── res/xml/network_security_config.xml
│   │       │   └── kotlin/com/waterleakage/app/
│   │       │       ├── MainActivity.kt
│   │       │       ├── data/
│   │       │       │   ├── model/ReadingModels.kt
│   │       │       │   ├── remote/ApiClient.kt
│   │       │       │   ├── remote/WaterLeakageApi.kt
│   │       │       │   └── repository/WaterLeakageRepository.kt
│   │       │       └── ui/
│   │       │           ├── components/
│   │       │           │   ├── CommonComponents.kt
│   │       │           │   └── ServerConfigDialog.kt
│   │       │           ├── navigation/
│   │       │           │   ├── AppNavHost.kt
│   │       │           │   └── Screen.kt
│   │       │           ├── screens/dashboard/
│   │       │           ├── screens/add/
│   │       │           ├── screens/result/
│   │       │           ├── screens/history/
│   │       │           ├── screens/details/
│   │       │           └── theme/
│   │       │               ├── Color.kt
│   │       │               ├── Theme.kt
│   │       │               └── Type.kt
│   │       └── test/kotlin/com/waterleakage/app/DataModelTest.kt
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml
│   └── settings.gradle.kts
```

---

## 11. Backend Installation Instructions

A pre-configured virtual environment (`.venv`) is already present in the workspace. If setting up from scratch on another machine:

```powershell
# 1. Create a Python 3.11 virtual environment
uv venv --python 3.11 .venv

# 2. Install dependencies
uv pip install -r backend/requirements.txt
```

---

## 12. Backend Startup Commands

### Method A: One-Click Startup (Recommended)
Double-click [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat) in the project folder.  
It automatically:
1. Detects connected Android phones and bridges port 8000 over USB (`adb reverse tcp:8000 tcp:8000`).
2. Starts a continuous background daemon that auto-bridges the USB connection whenever a phone is reconnected.
3. Starts the Uvicorn server on `http://0.0.0.0:8000`.

### Method B: Manual PowerShell Startup
```powershell
cd "D:\College\Sem 3\ML\CIE3 Project\WaterLeakageDetection"
$env:PYTHONPATH="."
.\.venv\Scripts\uvicorn.exe backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```

* **Interactive API Documentation**: [http://localhost:8000/docs](http://localhost:8000/docs)
* **Health Check**: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

## 13. Android Studio Setup Instructions

1. Open **Android Studio**.
2. Click **Open** and select `WaterLeakageDetection/android`.
3. Allow Gradle to synchronize dependencies.
4. Verify JDK 17 is selected in **Settings $\rightarrow$ Build, Execution, Deployment $\rightarrow$ Build Tools $\rightarrow$ Gradle $\rightarrow$ Gradle JDK**.
5. Select the **app** configuration and click **Run (Shift + F10)**.

---

## 14. How to Connect an Android Emulator

1. In Android Studio, launch any Android Virtual Device (AVD).
2. The Android Emulator maps your computer's `localhost` to `http://10.0.2.2:8000/`.
3. The Android app automatically detects when running on an emulator and configures `http://10.0.2.2:8000/` automatically.

---

## 15. How to Connect a Physical Android Device

### Option 1: Over USB Cable (Recommended Zero-Config Mode)
1. Enable **Developer Options** and **USB Debugging** on your phone:
   * Go to **Settings $\rightarrow$ About Phone $\rightarrow$ tap "Build Number" 7 times**.
   * Go to **Settings $\rightarrow$ Developer Options $\rightarrow$ turn on "USB Debugging"**.
2. Plug your phone into your PC via USB cable.
3. Run [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat) (keeps USB reverse active continuously).
4. Launch the Android app on your phone:
   * On physical phones, the app **automatically defaults to `http://127.0.0.1:8000/`** (USB Bridge) and connects immediately on launch.
   * If disconnected, tap the **"Auto-Connect"** button on the Dashboard, and the app probes all endpoints and reconnects in under 2 seconds.

---

### Option 2: Over Wi-Fi (Wireless Mode)
1. Connect phone and PC to the **same Wi-Fi network**.
2. Set your PC's Wi-Fi network to **Private** in Windows:
   * **Settings $\rightarrow$ Network & internet $\rightarrow$ Wi-Fi $\rightarrow$ click your connected network $\rightarrow$ select "Private network"**.
3. Allow port 8000 in Windows Firewall (run as Administrator in PowerShell):
   ```cmd
   netsh advfirewall firewall add rule name="FastAPI 8000" dir=in action=allow protocol=TCP localport=8000
   ```
4. In the app on your phone, tap the **Gear icon** $\rightarrow$ tap **"Wi-Fi LAN (.29.251)"** (or **"Auto-Detect"**) $\rightarrow$ tap **Test & Save**.

---

## 16. How to Configure the Backend IP Address in App

Inside the Android app:
1. On the **Dashboard**, tap the **Settings (gear)** icon in the top-right corner.
2. Select one of the quick options:
   * **Auto-Detect Active Server**: Probes USB (`127.0.0.1`), Wi-Fi (`192.168.29.251`), and Emulator (`10.0.2.2`) in parallel, selects the reachable backend, and saves it automatically.
   * **USB Cable (127.0.0.1)**: 1-tap preset for USB Debugging.
   * **Wi-Fi LAN (192.168.29.251)**: 1-tap preset for wireless home network.
   * **Emulator (10.0.2.2)**: 1-tap preset for Android Studio emulators.
   * **Custom Entry**: Type any IP or port manually.

---

## 17. API Endpoint Documentation

All endpoints are served under `/api/v1`.

### 1. Health Check
* **`GET /api/v1/health`**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "models_loaded": true,
  "database_connected": true,
  "timestamp": "2026-11-09T23:30:00Z"
}
```

### 2. Submit Reading & Run Prediction
* **`POST /api/v1/readings`**
* **Request**:
```json
{
  "pipe_id": "PIPE-104",
  "pressure": 32.5,
  "refill": 0,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "timestamp": "2026-11-09T22:30:00"
}
```
* **Response**:
```json
{
  "id": 1,
  "pipe_id": "PIPE-104",
  "pressure": 32.5,
  "refill": 0,
  "latitude": 12.9716,
  "longitude": 77.5946,
  "reading_timestamp": "2026-11-09T22:30:00",
  "prob_logistic_regression": 0.8842,
  "prob_decision_tree": 1.0,
  "prob_random_forest": 0.98,
  "prob_svm": 0.9412,
  "pred_logistic_regression": "Leak",
  "pred_decision_tree": "Leak",
  "pred_random_forest": "Leak",
  "pred_svm": "Leak",
  "average_probability": 0.9514,
  "final_prediction": "Leak",
  "created_at": "2026-11-09T22:30:01"
}
```

### 3. List Readings
* **`GET /api/v1/readings`**
* Query Parameters: `filter` ("Leak" | "No Leak"), `search`, `limit`, `offset`.

### 4. Get Reading by ID
* **`GET /api/v1/readings/{id}`**

### 5. Delete Reading
* **`DELETE /api/v1/readings/{id}`**

### 6. Dashboard Summary
* **`GET /api/v1/dashboard/summary`**

---

## 18. Database Information

* **Engine**: SQLite
* **Location**: `backend/water_leakage.db`
* **Persistence**: Preserved across restarts.
* **Columns**: `id`, `pipe_id`, `pressure`, `refill`, `latitude`, `longitude`, `reading_timestamp`, `prob_logistic_regression`, `prob_decision_tree`, `prob_random_forest`, `prob_svm`, `pred_logistic_regression`, `pred_decision_tree`, `pred_random_forest`, `pred_svm`, `average_probability`, `final_prediction`, `created_at`.

---

## 19. Model Artifact Information

Located inside `backend/artifacts/`:
1. **`scaler.joblib`**: Fitted `StandardScaler` for `[Pressure, Refill]`.
2. **`logistic_regression.joblib`**: Binary Logistic Regression estimator.
3. **`decision_tree.joblib`**: Binary Decision Tree estimator.
4. **`random_forest.joblib`**: 100-tree Random Forest estimator.
5. **`svm.joblib`**: Support Vector Classifier with `probability=True`.

---

## 20. Testing & Verification

### Running Backend Tests
Execute pytest with the project root in `PYTHONPATH`:
```powershell
$env:PYTHONPATH="."
.\.venv\Scripts\pytest.exe backend\tests\test_backend.py -v
```
* **Results**: 8 passed in ~5 seconds (verifies artifact loading, normal prediction, leak prediction, health check, reading creation, dashboard summary, deletion, and SQLite persistence).

### Running Android Tests
```powershell
cd android
.\gradlew.bat testDebugUnitTest
```
* **Results**: `BUILD SUCCESSFUL` (verifies data model transformations and business logic).

### Pre-Built Android APK
The compiled debug APK is located at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 21. How to Run the Application (Complete Walkthrough)

1. **Connect your phone** via USB cable (USB Debugging on).
2. **Double-click [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat)**.
3. **Install the APK** on your phone (if not already installed):
   ```powershell
   & "C:\Users\prajw\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "android\app\build\outputs\apk\debug\app-debug.apk"
   ```
4. **Open the app**; it will auto-detect the backend over the USB port bridge.
5. Tap **"New Reading"** and test sample values:
   * **Normal Test**: Pressure = `52.5`, Refill = `0` $\longrightarrow$ Result: **NO LEAK**
   * **Leak Test**: Pressure = `28.0`, Refill = `0` $\longrightarrow$ Result: **LEAK DETECTED**

---

## 22. Troubleshooting

* **Issue: "Unable to connect" over Wi-Fi**
  * *Solution*: Windows Wi-Fi network profile is set to Public by default. Switch Wi-Fi from **Public** to **Private** in Windows Settings and allow port 8000 in Windows Firewall.
  * *Even Easier*: Use the USB cable and click **USB Cable** in the app after running `run_backend.bat`.
* **Issue: "No module named uvicorn"**
  * *Solution*: Run `.\.venv\Scripts\uvicorn.exe` or use `run_backend.bat` rather than unattached Python.
* **Issue: Phone not detected via ADB**
  * *Solution*: Re-plug the USB cable, tap "Allow USB debugging from this computer" on your phone screen, and verify with `adb devices`.

---

## 23. Current Limitations and Future Improvements

* **Manual Data Entry**: Current release accepts manual operator entries. Future builds can add Bluetooth Low Energy (BLE) or LoRaWAN gateways for automated pipeline telemetry.
* **Additional Physical Sensors**: Incorporating flow rate (GPM), pipe acoustic frequency analysis, and temperature can improve classification in overlapping pressure zones.
* **Imbalance Handling**: The training dataset is ~73% normal and ~27% leak. Applying cost-sensitive weights (`class_weight='balanced'`) could improve recall on subtle micro-leaks.
