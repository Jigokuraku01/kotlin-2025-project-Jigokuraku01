| Kotlin Course Project    | 24.B10                      | Application Development |
| :----------------------- | --------------------------- | ------------------------ |
| Client–Server Game       | Kashenin Andrey Dmitrievich | 2025                     |

---

**Language:** [Русский](README.md) | **English**

---

## Description

This repository contains a **Tic‑Tac‑Toe** client–server game with shared game models and two clients:

- **Backend** — modular network game logic (server and console client).
- **Frontend** — Android app with Jetpack Compose UI and two build flavors: Client and Server.

Detailed documentation is available in [Documentation(RUS).pdf](Documentation(RUS).pdf).

## Project Structure

- **KotlinProjectBack/backend** — server and console client, shared game logic:
  - **general** — common game entities: `IGame`, `TicTacToeGame`, network DTOs.
  - **server** — server startup, connection handling, move exchange, game state control.
  - **client** — console client with network scan and server selection.
- **KotlinProjectFront/app** — Android client/server with Compose UI:
  - productFlavors: **client** and **server**
  - Gradle tasks `runClient` and `runServer` for quick device launch.

## Key Features

- Network Tic‑Tac‑Toe over LAN
- Server waits for connection and runs turn‑based gameplay
- Client can scan the local network and pick an available server
- Shared game logic and data model across both parts
- JSON exchange for moves and states

## Tech Stack

- **Language:** Kotlin
- **Build:** Gradle (Kotlin DSL)
- **Networking:** Ktor Network + TLS
- **Serialization:** kotlinx.serialization (JSON)
- **Async:** Kotlin Coroutines
- **Android:** Jetpack Compose, Android Gradle Plugin
- **Min Android API:** 24

## Quick Start

### Backend (console server/client)

Project is in `KotlinProjectBack/backend` and consists of three modules: `server`, `client`, `general`.

### Frontend (Android app)

Project is in `KotlinProjectFront`. Two flavors are available:
- `client` — game client
- `server` — game server

Run via Gradle tasks:
- `runClient`
- `runServer`

## Notes

- The project is intended for local network (LAN) play.
- Game logic is shared between server and client parts.
