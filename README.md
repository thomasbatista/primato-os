# Primato OS

A full-stack Progressive Web App (PWA) for construction project management, built and delivered for a real client.

## Overview

Primato OS streamlines construction workflows by providing two distinct user roles — **Manager** and **Worker** — each with their own set of responsibilities and views. The system covers the full lifecycle of a construction project, from creation and task assignment to daily activity reporting.

## Tech Stack

**Backend**
- Java 21 + Spring Boot
- PostgreSQL (Supabase)
- Spring Security + JWT authentication
- Role-based access control (RBAC)
- Cloudflare R2 (photo storage)
- Hosted on Render

**Frontend**
- React + Vite + TypeScript
- Tailwind CSS
- PWA (installable on mobile and desktop)
- Hosted on Cloudflare Pages

## Features

- 🔐 JWT authentication with dual roles (Manager / Worker)
- 🏗️ Full project lifecycle management
- 📋 Daily activity reporting by workers
- 📸 Photo upload and storage via Cloudflare R2
- 📱 Installable as a PWA on any device

## Notes

This is a production system delivered to a private client. The source code is available for portfolio review purposes only.
