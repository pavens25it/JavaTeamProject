# KEC Placement & Campus Recruitment Eligibility System
## Student 1: Application Host (Placement Analytics Engine)

Welcome to the **KEC Placement & Campus Recruitment Eligibility System** repository. This component is developed and maintained by **Student 1 (Application Host)** to execute candidate screening, eligibility calculations, multithreaded benchmarking, and file export on **Oracle Cloud Infrastructure (OCI) VM 1**.

---

### Table of Contents
1. [Team Architecture & Division of Responsibility](#1-team-architecture--division-of-responsibility)
2. [OCI VM 1 Provisioning & Environment Setup](#2-oci-vm-1-provisioning--environment-setup)
3. [Building the Application](#3-building-the-application)
4. [Database Configuration & Coordination with Student 2](#4-database-configuration--coordination-with-student-2)
5. [Running the Application](#5-running-the-application)
6. [Recruitment Categories & Eligibility Rules](#6-recruitment-categories--eligibility-rules)
7. [Network Troubleshooting & Connectivity Verification](#7-network-troubleshooting--connectivity-verification)
8. [Project Structure & Deliverables Summary](#8-project-structure--deliverables-summary)

---

### 1. Team Architecture & Division of Responsibility

| Role | Host Instance | Primary Responsibilities |
| :--- | :--- | :--- |
| **Student 1** *(You / This Repository)* | **OCI VM 1** (Application Host) | • Hosts Java Placement Analytics Engine.<br>• Executes multithreaded candidate evaluation (500+ records).<br>• CSV import, sanitization, and company shortlist export.<br>• String processing (`24ITR001`, `substring()`, `indexOf()`, `String.format()`).<br>• Connects via JDBC to Student 2's remote database. |
| **Student 2** *(Partner)* | **OCI VM 2** (Database Host) | • Hosts MySQL (Port 3306) or PostgreSQL (Port 5432).<br>• Manages DB schema, user permissions, and table indexes.<br>• Configures OCI Security Lists (Ingress Rules) & OS firewall on VM 2. |

> [!IMPORTANT]
> Student 1 does **not** host, configure, or administer the database server or VM 2's firewall. Student 1 accesses Student 2's database strictly as an external JDBC client using environment variables.

---

### 2. OCI VM 1 Provisioning & Environment Setup

Log into your **OCI VM 1** via SSH:
```bash
ssh -i /path/to/your-oci-private-key.key ubuntu@<OCI_VM_1_PUBLIC_IP>
```

#### Step 2.1: Update System Packages
```bash
sudo apt-get update && sudo apt-get upgrade -y
```
*(On Oracle Linux / RHEL: `sudo dnf update -y`)*

#### Step 2.2: Install OpenJDK 17 LTS
```bash
sudo apt-get install -y openjdk-17-jdk git curl
# Verify Java installation:
java -version
```
Expected output:
```text
openjdk version "17.0.x" ...
OpenJDK Runtime Environment (build 17.0.x+...)
```

#### Step 2.3: Install Apache Maven
```bash
sudo apt-get install -y maven
# Verify Maven installation:
mvn -version
```

---

### 3. Building the Application

#### Step 3.1: Clone the Repository on OCI VM 1
```bash
cd ~
git clone https://github.com/<your-repo>/kec-placement-engine.git
cd kec-placement-engine
```

#### Step 3.2: Run Unit Tests
```bash
mvn clean test
```
All 29 automated tests verify category weighting, active backlog gating ($> 2 = \text{NE}$), critical subject cutoffs, regex string validation, multithreading, and offline database fallback.

#### Step 3.3: Build Executable Fat JAR
```bash
mvn package -DskipTests
```
This produces the standalone runnable fat JAR at `target/kec-placement-engine-1.0.0.jar` bundled with all MySQL and PostgreSQL JDBC drivers.

---

### 4. Database Configuration & Coordination with Student 2

Student 1 reads database connection settings through **Linux Environment Variables** (with automatic fallback to `src/main/resources/application.properties`).

#### Configure Environment Variables on OCI VM 1:
```bash
# Set Student 2's VM 2 private IP (if in same VCN) or public IP:
export DB_HOST="10.0.0.25"             # Replace with Student 2's OCI VM 2 IP
export DB_PORT="3306"                  # 3306 for MySQL, 5432 for PostgreSQL
export DB_NAME="kec_placement_db"      # Database created by Student 2
export DB_USER="kec_placement_user"    # Database user provisioned by Student 2
export DB_PASSWORD="YourStrongPassword@2026"
export DB_TYPE="mysql"                 # "mysql" or "postgresql"
```

To persist these across SSH sessions:
```bash
echo 'export DB_HOST="10.0.0.25"' >> ~/.bashrc
echo 'export DB_PORT="3306"' >> ~/.bashrc
echo 'export DB_NAME="kec_placement_db"' >> ~/.bashrc
echo 'export DB_USER="kec_placement_user"' >> ~/.bashrc
echo 'export DB_PASSWORD="YourStrongPassword@2026"' >> ~/.bashrc
echo 'export DB_TYPE="mysql"' >> ~/.bashrc
source ~/.bashrc
```

---

### 5. Running the Application

#### Mode A: Interactive Console Menu (13 Options)
```bash
java -jar target/kec-placement-engine-1.0.0.jar
```
Interactive Menu Options:
```text
==========================================================================
        KEC PLACEMENT & CAMPUS RECRUITMENT ELIGIBILITY SYSTEM            
            Student 1 (Application Host) - OCI VM 1 Engine               
==========================================================================
MAIN MENU:
  1.  Test Database Connection (OCI VM 2)
  2.  Add Student
  3.  Enter Phase 1 Scores (Aptitude)
  4.  Enter Phase 2 Scores (Coding & Technical)
  5.  Enter Phase 3 Scores and Backlogs (Soft Skills & Backlogs)
  6.  Import CSV (Assessment Dataset)
  7.  View Students (Loaded Cohort)
  8.  Generate Placement Shortlists (Multithreaded Screening)
  9.  View Eligibility Results (Formatted Tables)
  10. Export Company Shortlists (Separate CSVs)
  11. Test String Processing (Regex, substring & indexOf)
  12. Run 500+ Candidate Benchmark
  13. Exit
--------------------------------------------------------------------------
```

#### Mode B: Non-Interactive Automated Batch Pipeline
Ideal for automated deployment, benchmarks, and submission proofs:
```bash
java -jar target/kec-placement-engine-1.0.0.jar --batch
```
This automatically:
1. Ingests 550 candidate records from `data/input/candidates_500.csv`.
2. Concurrently screens all 550 candidates across 3 recruitment tiers in parallel using `ShortlistWorker`.
3. Exports 3 company shortlist files to `data/output/`.
4. Probes Student 2's remote database; if online, synchronizes student records and eligibility results.

---

### 6. Recruitment Categories & Eligibility Rules

#### Category Specifications:
| Tier Category | Critical Subject Cutoff | Category Weights | Aggregate Minimum |
| :--- | :--- | :--- | :--- |
| **TIER-1 PRODUCT** | Coding $\ge$ 60.0% | Coding = 50% \| Aptitude = 30% \| Soft Skills = 20% | $\ge$ 70.0% |
| **CORE ENGINEERING** | Technical $\ge$ 50.0% | Technical = 40% \| Aptitude = 40% \| Soft Skills = 20% | $\ge$ 60.0% |
| **MASS RECRUITER** | Aptitude $\ge$ 40.0% | Aptitude = 50% \| Soft Skills = 30% \| Technical = 20% | $\ge$ 50.0% |

#### Gating & Band Assignment:
* **Active Backlog Gating**: `Active Backlogs > 2` strictly flags candidate as **`NE`** (Not Eligible / Barred, Rank `F`).
* **Placement Bands**:
  * **Super Dream**: Aggregate $85.0 - 100.0$ $\rightarrow$ Rank **A**
  * **Dream**: Aggregate $70.0 - 84.99$ $\rightarrow$ Rank **B**
  * **Regular**: Aggregate $50.0 - 69.99$ $\rightarrow$ Rank **C**
  * **Ineligible**: Below threshold or backlog-barred $\rightarrow$ Rank **F**

---

### 7. Network Troubleshooting & Connectivity Verification

If Menu Option 1 reports that Student 2's database is offline, execute these diagnostic probes from OCI VM 1:

#### 1. Check IP Reachability:
```bash
ping -c 4 $DB_HOST
```

#### 2. Test Port Reachability (Port 3306 or 5432):
```bash
nc -zv -w 5 $DB_HOST $DB_PORT
# Or using telnet:
telnet $DB_HOST $DB_PORT
```

#### 3. What to Coordinate with Student 2:
If the connection times out:
1. **OCI VCN Ingress Rule**: Student 2 must add an Ingress Rule in OCI Console under their Subnet Security List:
   * **Source CIDR**: VM 1 Private IP (e.g. `10.0.0.12/32`) or VCN CIDR (`10.0.0.0/16`).
   * **IP Protocol**: TCP.
   * **Destination Port Range**: `3306` (MySQL) or `5432` (PostgreSQL).
2. **OS Firewall**: Student 2 must allow traffic in `iptables` / `ufw` on VM 2:
   ```bash
   sudo ufw allow 3306/tcp
   ```
3. **Database Bind Address**: Student 2 must ensure MySQL binds to `0.0.0.0` in `/etc/mysql/mysql.conf.d/mysqld.cnf`:
   ```ini
   bind-address = 0.0.0.0
   ```
4. **User Grant**: Student 2 must grant remote access to your user:
   ```sql
   CREATE USER 'kec_placement_user'@'%' IDENTIFIED BY 'YourStrongPassword@2026';
   GRANT ALL PRIVILEGES ON kec_placement_db.* TO 'kec_placement_user'@'%';
   FLUSH PRIVILEGES;
   ```

---

### 8. Project Structure & Deliverables Summary

```text
├── pom.xml                                      # Maven configuration & build plugins
├── README.md                                    # Complete OCI VM 1 deployment guide
├── data/
│   ├── input/
│   │   └── candidates_500.csv                   # 550 synthetic candidate assessment dataset
│   └── output/
│       ├── shortlist_tier_1_product.csv         # 206 Tier-1 Product eligible candidates
│       ├── shortlist_core_engineering.csv       # 364 Core Engineering eligible candidates
│       └── shortlist_mass_recruiter.csv         # 505 Mass Recruiter eligible candidates
├── src/
│   ├── main/
│   │   ├── java/com/kec/placement/
│   │   │   ├── DBConnection.java                # [Deliverable] JDBC Remote Connector
│   │   │   ├── PlacementDAO.java                # [Deliverable] Student 2 DAO Layer
│   │   │   ├── ShortlistWorker.java             # [Deliverable] Multithreaded Screener
│   │   │   ├── FileHandler.java                 # [Deliverable] CSV Import/Export Engine
│   │   │   ├── MainApp.java                     # [Deliverable] 13-Option Central Application
│   │   │   ├── model/
│   │   │   │   ├── Student.java                 # Candidate Profile
│   │   │   │   ├── Assessment.java              # Phased assessment scores & backlogs
│   │   │   │   ├── RecruitmentTier.java         # Tier categories, weights, cutoffs
│   │   │   │   ├── PlacementBand.java           # Super Dream, Dream, Regular, Ineligible
│   │   │   │   └── EligibilityResult.java       # Evaluated candidate screening container
│   │   │   └── service/
│   │   │       ├── EligibilityEngine.java       # Screening calculation engine
│   │   │       └── StringSanitizer.java         # Register No validation & skill parsing
│   │   └── resources/
│   │       ├── application.properties.example   # JDBC template
│   │       └── kec_placement_schema.sql         # Integration schema reference
│   └── test/java/com/kec/placement/
│       ├── DBConnectionTest.java                # JDBC & offline fallback verification
│       ├── FileHandlerTest.java                 # CSV ingestion & export tests
│       ├── ShortlistWorkerTest.java             # 550-candidate concurrent worker test
│       ├── model/ModelTest.java                 # Domain model unit tests
│       └── service/
│           ├── EligibilityEngineTest.java       # Category calculations & backlog gating tests
│           └── StringSanitizerTest.java         # Regex & substring/indexOf parsing tests
└── target/
    └── kec-placement-engine-1.0.0.jar           # Executable Standalone Fat JAR
```
