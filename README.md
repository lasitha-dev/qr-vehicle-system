# QR Vehicle Management System

A Spring Boot + Thymeleaf application for managing vehicle QR codes at the University of Peradeniya.

## Features

- 🔐 **Authentication**: Form-based login + Google OAuth2
- 🚗 **Vehicle Management**: Add, update, and track vehicles
- 📱 **QR Code Generation**: Generate QR codes for students and staff
- 🔍 **Person Search**: Search students, staff, and visitors
- 📄 **Certificate Management**: Upload and manage vehicle certificates
- 👥 **Role-based Access**: Admin, Entry, Viewer, and Searcher roles

## Tech Stack

- **Backend**: Spring Boot 3.x
- **Frontend**: Thymeleaf + Bootstrap
- **Database**: MySQL (Spring Data JPA)
- **Security**: Spring Security + OAuth2
- **QR Generation**: ZXing library

## Project Structure

```
src/main/java/com/uop/qrvehicle/
├── config/           # Security & Web configuration
├── controller/       # MVC Controllers
├── service/          # Business logic
├── repository/       # Data access layer
├── model/            # JPA Entities
├── dto/              # Data Transfer Objects
└── security/         # Custom security components

src/main/resources/
├── templates/        # Thymeleaf HTML templates
├── static/           # CSS, JS, images
└── application.properties
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+

### Setup

1. Clone the repository
2. Configure database in `application.properties`
3. Run the application:

```bash
mvn spring-boot:run
```

4. Open http://localhost:8080

## License

University of Peradeniya - Information Technology Center

---

*Migrated from PHP to Spring Boot (2026)*
