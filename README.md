# Water Leakage Detection — Machine Learning System & Android App

A comprehensive Machine Learning college project and native Android application designed to detect pipeline water leakages using physical sensor readings (Pressure and Refill status) evaluated by an ensemble of four scikit-learn classification models.

---

## 1. Project Overview

The **Water Leakage Detection** project bridges industrial predictive maintenance with mobile software engineering. Reusing pre-trained models from an industrial multi-tank network dataset, this solution includes:
* A **FastAPI Python Backend** serving four trained scikit-learn models through an unweighted probability averaging ensemble.
* An **SQLite Database** (`water_leakage.db`) persisting physical readings, GPS coordinates, timestamps, individual model predictions, and ensemble probabilities.
* A native **Android Application** (Kotlin + Jetpack Compose + Material 3) featuring a live Dashboard, Add Reading form, Prediction Results breakdown, Reading History with status filtering, and Reading Details.
* A **One-Click Startup Script** (`run_backend.bat`) that automatically bridges connections to physical phones over USB and starts the server.

> [!NOTE]
> This application is built for simulated inspection and manual entry of pipe readings. It does not connect to real-time physical IoT hardware or telemetry sensors.

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
4. Deliver a REST API with persistent SQLite database storage.
5. Provide a production-grade Android mobile interface for engineers and evaluators.

---

## 4. Machine Learning Approach

The models evaluate two physical features to predict pipeline condition:
1. **Pressure**: Measured fluid pressure. Nominal normal operating levels typically lie in the **45.0 to 65.0** range. Readings below 45 (pressure loss) or above 65 (pressure surge) frequently indicate leaks.
2. **Refill**: Binary indicator (`0` or `1`) designating whether an automated pump is refilling the tank.

### Feature Normalization
Input features are scaled using a pre-fitted `StandardScaler`:
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

Rather than majority voting on binary labels, the ensemble calculates the **unweighted arithmetic mean of the predicted leak probabilities**:

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

### Android App
* **Language**: Kotlin 2.3+
* **UI Framework**: Jetpack Compose (Material 3)
* **Networking**: Retrofit 2, OkHttp 4, Gson
* **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
* **Navigation**: Jetpack Navigation Compose
* **Hardware APIs**: Android Location Services & Permissions

---

## 9. Complete Project Folder Structure

```text
WaterLeakageDetection/
├── README.md                           # Comprehensive documentation
├── run_backend.bat                     # One-click startup script (starts server & bridges USB)
├── ml.ipynb                            # Original reference Google Colab notebook
│
├── backend/                            # FastAPI Python Backend
│   ├── app/
│   │   ├── config.py                   # App settings, DB path, artifacts dir
│   │   ├── database.py                 # SQLite SQLAlchemy engine & session
│   │   ├── main.py                     # FastAPI app instance, CORS & lifecycle
│   │   ├── models.py                   # Reading SQLAlchemy ORM model
│   │   ├── schemas.py                  # Pydantic v2 request/response schemas
│   │   ├── routers/
│   │   │   ├── dashboard.py            # GET /dashboard/summary
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
│   │       │           ├── components/CommonComponents.kt
│   │       │           ├── components/ServerConfigDialog.kt
│   │       │           ├── navigation/AppNavHost.kt
│   │       │           ├── navigation/Screen.kt
│   │       │           ├── screens/dashboard/
│   │       │           ├── screens/add/
│   │       │           ├── screens/result/
│   │       │           ├── screens/history/
│   │       │           ├── screens/details/
│   │       │           └── theme/
│   │       └── test/kotlin/com/waterleakage/app/DataModelTest.kt
│   ├── build.gradle.kts
│   ├── gradle/libs.versions.toml
    └── settings.gradle.kts
```

---

## 10. Backend Installation Instructions

A pre-configured virtual environment (`.venv`) is already present in the workspace. If setting up from scratch on another machine:

```powershell
# 1. Create a Python 3.11 virtual environment
uv venv --python 3.11 .venv

# 2. Install dependencies
uv pip install -r backend/requirements.txt
```

---

## 11. Backend Startup Commands

### Method A: One-Click Startup (Recommended)
Double-click [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat) in the project folder.  
It automatically:
1. Detects connected Android phones and bridges port 8000 over USB (`adb reverse tcp:8000 tcp:8000`).
2. Starts the Uvicorn server on `http://0.0.0.0:8000`.

### Method B: Manual PowerShell Startup
```powershell
cd "D:\College\Sem 3\ML\CIE3 Project\WaterLeakageDetection"
$env:PYTHONPATH="."
.\.venv\Scripts\uvicorn.exe backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```

* **API Documentation**: [http://localhost:8000/docs](http://localhost:8000/docs)
* **Health Check**: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

## 12. Android Studio Setup Instructions

1. Open **Android Studio**.
2. Click **Open** and select `WaterLeakageDetection/android`.
3. Allow Gradle to synchronize dependencies.
4. Verify JDK 17 is selected in **Settings $\rightarrow$ Build, Execution, Deployment $\rightarrow$ Build Tools $\rightarrow$ Gradle $\rightarrow$ Gradle JDK**.
5. Select the **app** configuration and click **Run (Shift + F10)**.

---

## 13. How to Connect an Android Emulator

1. In Android Studio, launch any Android Virtual Device (AVD).
2. The Android Emulator maps your computer's `localhost` to `http://10.0.2.2:8000/`.
3. The Android app defaults to `http://10.0.2.2:8000/`, so emulator connections work out of the box.

---

## 14. How to Connect a Physical Android Device

### Option 1: Over USB Cable (Easiest & Most Reliable)
1. Enable **Developer Options** and **USB Debugging** on your phone:
   * Go to **Settings $\rightarrow$ About Phone $\rightarrow$ tap "Build Number" 7 times**.
   * Go to **Settings $\rightarrow$ Developer Options $\rightarrow$ turn on "USB Debugging"**.
2. Plug your phone into your PC via USB cable.
3. Run [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat) (or run `adb reverse tcp:8000 tcp:8000`).
4. In the Android app on your phone, tap the **Gear icon (top right)** $\rightarrow$ tap **Localhost** (`http://localhost:8000/`) $\rightarrow$ tap **Test & Save**.
5. The phone routes all requests directly through the USB cable, completely bypassing router and firewall issues.

---

### Option 2: Over Wi-Fi (Wireless)
1. Connect phone and PC to the **same Wi-Fi network**.
2. Set your PC's Wi-Fi network to **Private** in Windows:
   * **Settings $\rightarrow$ Network & internet $\rightarrow$ Wi-Fi $\rightarrow$ click your connected network $\rightarrow$ select "Private network"**.
3. Allow port 8000 in Windows Firewall (run as Administrator in PowerShell):
   ```cmd
   netsh advfirewall firewall add rule name="FastAPI 8000" dir=in action=allow protocol=TCP localport=8000
   ```
4. Find your PC's IP address (`ipconfig` $\rightarrow$ e.g., `192.168.29.251`).
5. In the app on your phone, tap the **Gear icon** $\rightarrow$ enter `http://192.168.29.251:8000/` $\rightarrow$ tap **Test & Save**.

---

## 15. How to Configure the Backend IP Address in App

Inside the Android app:
1. On the **Dashboard**, tap the **Settings (gear)** icon in the top-right corner.
2. Select one of the quick presets or type a custom address:
   * **Localhost Button**: Sets `http://localhost:8000/` (for USB debugging via `adb reverse`).
   * **Emulator Button**: Sets `http://10.0.2.2:8000/` (for standard Android emulators).
   * **Custom Wi-Fi**: Enter `http://<your-pc-ip>:8000/` (e.g. `http://192.168.29.251:8000/`).
3. Tap **Test & Save**. The app sends a test request to `/api/v1/health` and displays a green confirmation upon success.

---

## 16. API Endpoint Documentation

All endpoints are served under `/api/v1`.

### 1. Health Check
* **`GET /api/v1/health`**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "models_loaded": true,
  "database_connected": true,
  "timestamp": "2026-11-05T23:30:00Z"
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
  "timestamp": "2026-11-05T22:30:00"
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
  "reading_timestamp": "2026-11-05T22:30:00",
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
  "created_at": "2026-11-05T22:30:01"
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

## 17. Database Information

* **Engine**: SQLite
* **Location**: `backend/water_leakage.db`
* **Persistence**: Preserved across restarts.
* **Columns**: `id`, `pipe_id`, `pressure`, `refill`, `latitude`, `longitude`, `reading_timestamp`, `prob_logistic_regression`, `prob_decision_tree`, `prob_random_forest`, `prob_svm`, `pred_logistic_regression`, `pred_decision_tree`, `pred_random_forest`, `pred_svm`, `average_probability`, `final_prediction`, `created_at`.

---

## 18. Model Artifact Information

Located inside `backend/artifacts/`:
1. **`scaler.joblib`**: Fitted `StandardScaler` for `[Pressure, Refill]`.
2. **`logistic_regression.joblib`**: Binary Logistic Regression estimator.
3. **`decision_tree.joblib`**: Binary Decision Tree estimator.
4. **`random_forest.joblib`**: 100-tree Random Forest estimator.
5. **`svm.joblib`**: Support Vector Classifier with `probability=True`.

---

## 19. How to Run the Application (Complete Walkthrough)

1. **Connect your phone** via USB cable (USB Debugging on).
2. **Double-click [`run_backend.bat`](file:///d:/College/Sem%203/ML/CIE3%20Project/WaterLeakageDetection/run_backend.bat)**.
3. **Install the APK** on your phone (if not already installed):
   ```powershell
   & "C:\Users\prajw\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r "android\app\build\outputs\apk\debug\app-debug.apk"
   ```
4. **Open the app**, tap the **Gear icon** $\rightarrow$ tap **Localhost** $\rightarrow$ tap **Test & Save**.
5. Tap **"+ New Reading"** and enter:
   * **Normal Test**: Pressure = `52.5`, Refill = `0` $\rightarrow$ Result: **NO LEAK**
   * **Leak Test**: Pressure = `28.0`, Refill = `0` $\rightarrow$ Result: **LEAK DETECTED**

---

## 20. Troubleshooting

* **Issue: "Unable to connect" over Wi-Fi**
  * *Solution*: Windows Wi-Fi network profile is set to Public by default. Switch Wi-Fi from **Public** to **Private** in Windows Settings and allow port 8000 in Windows Firewall.
  * *Even Easier*: Use the USB cable and click **Localhost** in the app after running `run_backend.bat`.
* **Issue: "No module named uvicorn"**
  * *Solution*: Run `.\.venv\Scripts\uvicorn.exe` or use `run_backend.bat` rather than unattached Python.
* **Issue: Phone not detected via ADB**
  * *Solution*: Re-plug the USB cable, tap "Allow USB debugging from this computer" on your phone screen, and verify with `adb devices`.

---

## 21. Current Limitations and Future Improvements

* **Manual Data Entry**: Current release accepts manual operator entries. Future builds can add Bluetooth Low Energy (BLE) or LoRaWAN gateways for automated pipeline telemetry.
* **Additional Physical Sensors**: Incorporating flow rate (GPM), pipe acoustic frequency analysis, and temperature can improve classification in overlapping pressure zones.
* **Imbalance Handling**: The training dataset is ~73% normal and ~27% leak. Applying cost-sensitive weights (`class_weight='balanced'`) could improve recall on subtle micro-leaks.
