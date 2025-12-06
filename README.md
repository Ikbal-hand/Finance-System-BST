# 📱 CV BST Finance App

![Banner](https://via.placeholder.com/1200x300.png?text=CV+BST+Finance+App+Banner)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blueviolet" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-9cf" />
  <img src="https://img.shields.io/badge/Database-Room-blue" />
  <img src="https://img.shields.io/badge/PDF-Native%20PdfDocument-lightgrey" />
</p>

---

## ⚠️ PRIVATE REPOSITORY / PROPRIETARY SOFTWARE

This application is developed exclusively for the internal operations of **CV. BERKARYA SATU TUJUAN**.  
Distribution or use of the source code without written permission is strictly prohibited.

---

## 📖 About the Application

CV BST Finance App is a financial and operational management solution built with **Android Native (Kotlin)**.  
It solves multi-branch business management challenges through advanced security, automated reporting, and high data integrity.

The system replaces error-prone manual bookkeeping with automated cashflow separation algorithms between branches and headquarters.

---

## 🎯 Business Solutions & Value

### **1. Financial Accuracy (Anti Double-Counting)**  
**Problem:** Internal transfers often appear as false revenue.  
**Solution:** The `FinanceCalculator` algorithm strictly distinguishes between **Real Revenue** and **Internal Transfers**.

### **2. Real-Time Visibility**  
Instant aggregated data from all branches without needing end-of-day recap.

### **3. Payroll Efficiency**  
Automated payslip generator creates PDF salary slips and sends them directly to employees’ WhatsApp.

### **4. Data Security (Zero Data Loss)**  
Dual-layer backup (Local + Google Drive) runs automatically every midnight.

### **5. Access Protection**  
Fingerprint authentication + encrypted PIN prevents unauthorized access.

---

## ✨ Key Technical Features

### **1. Multi-Branch Finance Management**
- Automatic separation of Petty Cash (Branches) and Main Cash (Head Office) at database level.  
- Daily performance charts using **MPAndroidChart**.

### **2. Native PDF Generation**
- Lightweight PDF creation using **Android Canvas API**.  
- Consolidated, print-ready monthly reports.

### **3. WhatsApp Integration & Smart Notifications**
- Send PDF documents directly to specific WhatsApp numbers without saving contacts.  
- Alerts for minimum balance & backup failures.

### **4. Security & Cloud Sync**
- Background synchronization via **WorkManager**.  
- Encrypted credential & PIN storage using modern Android security standards.

---

## 🛠️ Technology Stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose (Material Design 3)  
- **Architecture:** MVVM  
- **Database:** Room (SQLite)  
- **Cloud Storage:** Google Drive REST API  
- **Authentication:** Biometric API + Google Sign-In  
- **Background Tasks:** WorkManager  
- **PDF Engine:** Native PdfDocument + Canvas  
- **Dependency Injection:** Manual ViewModelFactory  

---

## 📂 Module Structure

```
com.loganes.finace
├── data/                     # Data Layer (Room DAO, Repositories)
│   ├── BackupManager.kt      # Local Backup & Restore Logic
│   ├── GoogleDriveHelper.kt  # Google Drive API Integration
│   └── FinanceCalculator.kt  # Core Financial Logic
├── model/                    # Data Classes & Entities
├── ui/                       # Presentation Layer (Jetpack Compose)
│   ├── components/           # Reusable Components
│   └── screens/              # Dashboard, Transactions, etc.
├── viewmodel/                # State Management
└── workers/                  # Background Jobs (AutoBackupWorker)
```

---

## 🚀 Development Setup

### 1. Clone the Repository  
Ensure your GitHub/GitLab account has been granted access to this private repository.

### 2. Configure API Keys  
The following files are **not included** for security reasons:
- `google-services.json`
- `credentials.json` (Google Drive API)

Contact the Lead Developer to obtain these configuration files.

### 3. Build the Project  
- Use the latest Android Studio (Hedgehog / Iguana+).  
- Minimum supported Android version: **8.0 (Oreo)**.

---

## 📞 Support & Maintenance

- **Developer:** Loganes IT Team  
- **Email:** ikbalhand13@gmail.com 

---

## 🔒 Copyright & Licensing

© 2024 CV. Bangun Sejahtera & Loganes IT Team.  
All rights reserved.

This application is **Proprietary Software**.  
The source code, interface design, and business logic are intellectual property of **CV. BERKARYA SATU TUJUAN**.

**Strictly prohibited without written permission:**
- Copying  
- Redistributing  
- Modifying  
- Selling  

Usage is permitted only on official devices owned by CV. BERKARYA SATU TUJUAN.
