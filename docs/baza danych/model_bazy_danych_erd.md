# Model Bazy Danych (ERD) — Mobilny Pacjent

Dokumentacja schematu relacyjnego PostgreSQL dla platformy Supabase. Model uwzględnia wymagania bezpieczeństwa aplikacji mobilnej (izolacja tożsamości, audytowalność, kontrola dostępu RLS).

---

## 1. Diagram Relacji (Mermaid ERD)

```mermaid
erDiagram
    %% Schemat Auth w Supabase
    "auth.users" ||--|| profiles : "id = id (1:1)"
    
    %% Relacje profili
    profiles ||--o{ doctor_patients : "lekarz (doctor_id)"
    profiles ||--o{ doctor_patients : "pacjent (patient_id)"
    profiles ||--o{ appointments : "pacjent (patient_id)"
    profiles ||--o{ appointments : "lekarz (doctor_id)"
    profiles ||--o{ medical_records : "pacjent (patient_id)"
    profiles ||--o{ medical_records : "lekarz (doctor_id)"
    profiles ||--o{ prescriptions : "pacjent (patient_id)"
    profiles ||--o{ prescriptions : "lekarz (doctor_id)"
    profiles ||--o{ medical_files : "pacjent (patient_id)"
    profiles ||--o{ audit_logs : "autor akcji (actor_id)"

    %% Relacje placówek i wizyt
    clinics ||--o{ appointments : "miejsce wizyty (clinic_id)"
    appointments ||--o| medical_records : "wpis z wizyty (appointment_id)"

    %% Relacje dokumentacji i załączników
    medical_records ||--o{ prescriptions : "powiązana e-recepta"
    medical_records ||--o{ medical_files : "załączniki badania"

    "auth.users" {
        uuid id PK "Zarządzane przez Supabase Auth"
        string email "Poufne"
        timestamptz created_at
    }

    profiles {
        uuid id PK, FK "Referencja do auth.users(id)"
        string role "CHECK: patient / doctor / admin"
        string first_name "Imię"
        string last_name "Nazwisko"
        string pesel "Poufne / UNIQUE"
        string phone "Telefon kontaktowy"
        string address "Adres zamieszkania"
        timestamptz created_at
    }

    clinics {
        uuid id PK
        string name "Nazwa placówki (Publiczne)"
        string address "Adres fizyczny"
        float latitude "Współrzędna GPS"
        float longitude "Współrzędna GPS"
        string phone "Telefon rejestracji"
    }

    doctor_patients {
        uuid id PK
        uuid doctor_id FK "Lekarz prowadzący"
        uuid patient_id FK "Przypisany pacjent"
        timestamptz created_at
    }

    appointments {
        uuid id PK
        uuid patient_id FK "Pacjent"
        uuid doctor_id FK "Lekarz"
        uuid clinic_id FK "Placówka medyczna"
        timestamptz scheduled_at "Termin wizyty"
        string status "CHECK: scheduled / completed / cancelled"
        string notes "Uwagi do rejestracji"
        timestamptz created_at
    }

    medical_records {
        uuid id PK
        uuid patient_id FK "Właściciel dokumentacji"
        uuid doctor_id FK "Autor wpisu"
        uuid appointment_id FK "Opcjonalne ID wizyty"
        string record_type "CHECK: visit / test_result / referral"
        string description "Szczególnie wrażliwe (diagnoza/wywiad)"
        timestamptz created_at
    }

    prescriptions {
        uuid id PK
        uuid patient_id FK "Pacjent"
        uuid doctor_id FK "Lekarz wystawiający"
        uuid medical_record_id FK "Powiązany wpis historii"
        string code "Kod e-recepty (4 cyfry)"
        string medications "Wykaz leków i dawkowanie"
        timestamptz issued_at
        date valid_until
        string status "CHECK: active / fulfilled / expired / cancelled"
    }

    medical_files {
        uuid id PK
        uuid patient_id FK "Pacjent"
        uuid uploaded_by FK "Autor uploadu (lekarz/pacjent)"
        uuid medical_record_id FK "Powiązana dokumentacja"
        string storage_path "Ścieżka w Supabase Storage"
        string file_name "Oryginalna nazwa pliku"
        string mime_type "image/jpeg, application/pdf"
        timestamptz created_at
    }

    audit_logs {
        uuid id PK
        uuid actor_id FK "Użytkownik wykonujący operację"
        string action "Typ akcji (CREATE_RECORD, READ_DATA, LOGIN)"
        string entity_type "Nazwa tabeli lub zasobu"
        uuid entity_id "ID modyfikowanego rekordu"
        timestamptz created_at "Znak czasu (Append-Only)"
    }
```

---

## 2. Podsumowanie architektury tabel i kluczy

| Nazwa tabeli | Rola w systemie | Klasyfikacja danych | Zabezpieczenie RLS |
|---|---|---|---|
| **`auth.users`** | Wewnętrzny schemat Supabase Auth (hashe haseł, sesje). | Poufne | Całkowity brak bezpośredniego dostępu z aplikacji mobilnej. |
| **`profiles`** | Zunifikowana tabela użytkowników (`role`: pacjent/lekarz/admin) + dane osobowe i PESEL. | Poufne | Odczyt: profil własny; lekarz widzi profile swoich pacjentów; modyfikacja: wyłącznie profil własny (bez zmiany roli). |
| **`clinics`** | Katalog placówek medycznych ze współrzędnymi GPS dla modułu mapy. | Publiczne | Odczyt: publiczny (nawet bez logowania); zapis/edycja: wyłącznie rola `admin`. |
| **`doctor_patients`** | Asocjacja lekarz–pacjent (klucz do weryfikacji BOLA/IDOR). | Poufne | Odczyt i tworzenie powiązań ściśle kontrolowane przez lekarza i admina. |
| **`appointments`** | Terminy i statusy wizyt lekarskich powiązane z konkretną placówką. | Poufne | Dostęp: pacjent do swoich wizyt, lekarz do wizyt w swoim kalendarzu. |
| **`medical_records`** | Historia zdrowia, wywiady, opisy badań i diagnozy medyczne. | **Szczególnie wrażliwe** | Odczyt: pacjent widzi wyłącznie swoje; lekarz wyłącznie dla przypisanych pacjentów (`doctor_patients`). |
| **`prescriptions`** | E-recepty i kody odbioru leków. | Poufne | Wgląd dla pacjenta i wystawiającego lekarza; edycja zabroniona po wystawieniu. |
| **`medical_files`** | Metadane plików binarnych (skany, PDF-y badań) trzymanych w Supabase Storage. | **Szczególnie wrażliwe** | Ściśle powiązane z uprawnieniami do prywatnego bucketu w Storage. |
| **`audit_logs`** | Dziennik zdarzeń weryfikujący niezaprzeczalność (STRIDE: Repudiation). | Wewnętrzne | Tabela **Append-Only** (tylko `INSERT`); brak uprawnień `UPDATE` i `DELETE` dla jakiegokolwiek użytkownika. |

---

## 3. Kluczowe reguły integralności i indeksy SQL

1. **Unikalność relacji lekarz–pacjent:**
   * Ograniczenie `UNIQUE(doctor_id, patient_id)` w tabeli `doctor_patients` eliminuje dublowanie relacji.
2. **Kaskadowe usuwanie profili:**
   * Klucz `profiles.id REFERENCES auth.users(id) ON DELETE CASCADE` gwarantuje spójność po skasowaniu konta w Auth.
3. **Optymalizacja zapytań (Indeksy):**
   * Indeksy na `appointments(patient_id, scheduled_at)` oraz `appointments(doctor_id, scheduled_at)` dla szybkiego filtrowania kalendarza wizyt.
4. **Ograniczenie ról w aplikacji:**
   * Walidacja wartości kolumny `role` przez warunek SQL: `CHECK (role IN ('patient', 'doctor', 'admin'))`.