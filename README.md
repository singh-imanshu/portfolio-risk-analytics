# Portfolio Risk Analytics
---

This repository acts as the root container for the Portfolio Risk Analytics platform. The application is divided into two Git submodules.

## Project Structure
---

* **`risque_frontend`:** The React/Vite client interface.
* **`risque_backend`:** The Java Spring Boot REST API.

## Setup
---

### 1. Clone the Project

To clone this repository and automatically pull the code for both the frontend and backend submodules, use the `--recurse-submodules` flag:

```bash
git clone --recurse-submodules [https://github.com/singh-imanshu/portfolio-risk-analytics.git](https://github.com/singh-imanshu/portfolio-risk-analytics.git)
```

### 2. Update an Existing Clone

If you have already cloned the repository without pulling the submodules, initialize and update them by running:

```bash
git submodule update --init --recursive
```
