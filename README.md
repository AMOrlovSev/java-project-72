### Hexlet tests and linter status:
[![Actions Status](https://github.com/AMOrlovSev/java-project-72/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/AMOrlovSev/java-project-72/actions)

### GitHub Actions:
[![Java CI](https://github.com/AMOrlovSev/java-project-72/actions/workflows/JavaCI.yml/badge.svg)](https://github.com/AMOrlovSev/java-project-72/actions/workflows/JavaCI.yml)

### SonarQube:
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_java-project-72&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_java-project-72)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=AMOrlovSev_java-project-72&metric=coverage)](https://sonarcloud.io/summary/new_code?id=AMOrlovSev_java-project-72)


# 📊 Web Page Analyzer (Java Project 72)

---

## 🚀 Project Overview

This is a lightweight web application designed for **analyzing and tracking key SEO parameters** of specified URLs.

Users can add websites for monitoring and run checks on them. The application performs an HTTP request to the page and saves the following information:

1. **Status code** of the server response (e.g., 200, 404, 500)
2. **Page title** (`<title>`)
3. **Text of the first H1 tag**
4. **Meta description** (`<meta name="description">`)

All check results are saved in the database, allowing users to track changes in status and content over time.

---

## 🛠️ Technology Stack

The project is developed in **Java** and uses a modern, high-performance stack:

| Purpose | Technology | Description |
|---------|------------|-------------|
| **Web Framework** | **Javalin** | Lightweight and fast framework for routing and HTTP handling |
| **Database** | **PostgreSQL** / H2 | PostgreSQL in production; H2 for development and testing |
| **Connection Pool** | **HikariCP** | High-performance database connection pool |
| **Template Engine** | **JTE** | Java Template Engine for rendering HTML interface |
| **HTTP Client** | **Unirest** | Used for making external requests to analyzed websites |
| **HTML Parser** | **Jsoup** | Library for extracting SEO data (title, h1, description) from HTML |
| **Testing** | **JUnit 5, MockWebServer** | Integration and unit testing, including mocking external HTTP requests |
---

## 🌐 Live Demo:
 [**View deployed application on Render**](https://java-project-72-iaqe.onrender.com)
