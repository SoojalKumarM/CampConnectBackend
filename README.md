# Campus Connect — Backend API

> A robust, secure, and privacy-first REST API built for verified university students, powering the Campus Connect platform.

---

## 🌟 Highlights

- **Robust Authentication**: Secure credential management via BCrypt, server-side Google ID-token verification, and stateless JWT session handling.
- **Privacy by Design**: User contact details (such as WhatsApp numbers) are securely stored and exposed **only** after a mutual connection request is `ACCEPTED`—enforced centrally via `ProfileMapper`.
- **Verified Profiles**: Complete profile lifecycle management including photo uploads, interests, intent tagging, visibility toggles, and email domain-based student verification.
- **Discovery & Compatibility**: Dynamic filtering, pagination, and a real-time compatibility score algorithm.
- **Connection Lifecycle Management**: Full request workflow (send, accept, decline, cancel, remove), automated mutual-interest matching, daily request limits, and decline cooldowns.
- **Trust & Safety Suite**: Blocking, reporting, account deactivation, in-app notifications, and asynchronous email alerts.

---

## 🛠️ Tech Stack

- **Language & Core**: Java 17, Spring Boot 3.3 (Web, Security, Data JPA, Validation, Mail)
- **Persistence**: Hibernate 6, MySQL 8
- **Security & Integration**: jjwt (JSON Web Tokens), Google API Client
- **Utilities & Build**: Lombok, Apache Maven

---

## 🏗️ Architecture & Project Structure

The application follows a strict layered architecture pattern where DTOs handle the API contract, keeping underlying database entities isolated:

```text
HTTP ─▶ Security filters (CORS, JWT) ─▶ Controller ─▶ Service ─▶ Repository ─▶ MySQL
                                       (DTOs in/out)  (business rules,  (Spring Data
                                                      @Transactional)   JPA/Hibernate)