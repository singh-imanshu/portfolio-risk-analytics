# Portfolio Risk Analytics
---

This repository serves as the root container for the Portfolio Risk Analytics platform. To maintain a clean separation of concerns, the project utilizes a micro-repository architecture managed via Git submodules. 

## What it does
---

* **Portfolio Management**: Users can create portfolios and manage individual stock holdings[cite: 1, 2]. 
* **Market Data Integration**: The backend integrates with the Alpha Vantage API to retrieve up-to-date stock data[cite: 2].
* **Risk Analysis**: The platform evaluates portfolios and calculates specific risk metrics[cite: 2]. It displays these results visually on a dedicated analytics dashboard[cite: 1].
* **Secure Access**: User sessions are secured using JWT (JSON Web Tokens) authentication[cite: 2].
* **User Notifications**: The system includes backend email services[cite: 2]. It also handles OTP (One-Time Password) requests for account verification[cite: 2].
* **AI Capabilities**: The backend includes configuration for a chat client, facilitating AI-driven portfolio analysis[cite: 2].

## The Stack
---

* **Frontend**: Built with React and Vite[cite: 1]. It features custom hooks for state management and a component-based UI that includes modals, stat cards, and sidebars[cite: 1].
* **Backend**: Developed using Java and Spring Boot[cite: 2]. It utilizes Maven for dependency management and follows a structured controller-service-repository architecture[cite: 2].

## Setup
---

Because this repository relies on Git submodules, a standard Git clone operation will only pull the root directory structure and leave the submodule folders empty. Follow the instructions below to properly initialize the entire project workspace.

### 1. Initial Clone

To clone this repository and automatically fetch the underlying source code for both the frontend and backend submodules in a single step, use the `--recurse-submodules` flag:

```bash
git clone --recurse-submodules [https://github.com/singh-imanshu/portfolio-risk-analytics.git](https://github.com/singh-imanshu/portfolio-risk-analytics.git)
