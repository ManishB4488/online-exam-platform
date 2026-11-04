# Online Exam Platform

A full stack web app where admins create exams and students take them with a countdown timer. The score is counted on the server, and the exam time is also checked on the server.

## Features

**Student**
- Register and log in
- See available exams and start one
- Answer multiple-choice questions with a countdown timer (the exam submits by itself when time is over)
- See the score with a review of right and wrong answers
- See all past results

**Admin**
- Create, edit and delete exams
- Add, edit and delete questions
- See the results of all students and filter them by exam

**Security**
- Passwords are saved as BCrypt hashes
- JWT tokens for login
- Role-based access: students cannot open admin pages or admin APIs
- Students never receive the correct answers before they submit

## Tech stack

| Part | Technology |
|---|---|
| Frontend | React 19, Vite, React Router, Axios |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA |
| Database | H2 (file database for development) |
| Auth | JWT (jjwt) |
| Tests | JUnit 5, Mockito, AssertJ |

## Project structure

```
online-exam/
├── backend/exam-backend/   Spring Boot REST API
│   └── src/main/java/com/manish/exam_backend/
│       ├── config/         security, CORS, error handling, sample data
│       ├── controller/     REST endpoints
│       ├── dto/            request and response objects
│       ├── model/          database entities
│       ├── repository/     database access
│       ├── security/       JWT service and filter
│       └── service/        business logic
└── frontend/               React app
    └── src/
        ├── api/            axios client and error messages
        ├── components/     Navbar, ProtectedRoute
        ├── context/        login state
        ├── pages/          all screens
        └── utils/
```

## Run it on your computer
### With Docker
```bash
copy .env.example .env      # then set DB_PASSWORD and JWT_SECRET in .env
docker compose up -d --build
```
Open http://localhost:3000
You need Java 21, Maven and Node.js 18 or newer.

**1. Backend**
```bash
cd backend/exam-backend
mvn spring-boot:run
```
It runs on http://localhost:8080

**2. Frontend** (in a second terminal)
```bash
cd frontend
npm install
npm run dev
```
It runs on http://localhost:5173

## Sample accounts (for development only)

| Role | Email | Password |
|---|---|---|
| Admin | admin@exam.com | admin123 |
| Student | student@exam.com | student123 |

New accounts made on the register page are always students.

## Settings

| Setting | Where | Default |
|---|---|---|
| `JWT_SECRET` | environment variable (at least 32 characters) | a development value |
| `VITE_API_URL` | `frontend/.env` | http://localhost:8080 |

Always set your own `JWT_SECRET` when you deploy the app.

## API overview

| Method | Endpoint | Who | What it does |
|---|---|---|---|
| POST | /api/auth/register | everyone | Create a student account |
| POST | /api/auth/login | everyone | Log in and get a token |
| GET | /api/student/exams | student | List exams that have questions |
| GET | /api/student/exams/{id}/start | student | Start or continue an exam |
| POST | /api/student/exams/{id}/submit | student | Submit answers and get the score |
| GET | /api/student/results | student | My past results |
| GET, POST | /api/admin/exams | admin | List or create exams |
| GET, PUT, DELETE | /api/admin/exams/{id} | admin | Read, edit or delete an exam |
| GET, POST | /api/admin/exams/{id}/questions | admin | List or add questions |
| PUT, DELETE | /api/admin/questions/{id} | admin | Edit or delete a question |
| GET | /api/admin/results | admin | Results of all students |

## Run the tests

```bash
cd backend/exam-backend
mvn test
```

## Roadmap

DevOps work planned for this project: Docker and Docker Compose, a Jenkins CI/CD pipeline, automated tests and security scans in the pipeline, automatic deployment, and monitoring.

## Author

Manish Bisht