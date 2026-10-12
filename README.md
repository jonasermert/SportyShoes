# SportyShoes

SportyShoes is a complete stakeholder prototype for the original Simplilearn Full Stack Java project. The [original brief](docs/ORIGINAL_BRIEF.md) is preserved, and the [specification](docs/SPECIFICATION.md) describes capabilities, appearance, interactions and Java concepts.

The React and Tailwind frontend includes a responsive catalogue, category filtering, account registration, sign-in, simulated checkout and personal purchase history. The protected administrator area supports product creation and editing, categorization, archiving, user browsing and search, inclusive date/category purchase reports and password changes. Spring Boot, Spring Security and JPA implement persistent data, role checks, BCrypt passwords, CSRF protection and transactional stock deduction.

## Run locally

Use Java 17, Maven 3.8+ and Node 24 with npm. The Maven project targets Java 11 bytecode to retain compatibility with the original project, while CI runs on Java 17.

```sh
mvn spring-boot:run
```

In a second terminal, start the frontend development server. Vite forwards API calls to Spring Boot on port 8080.

```sh
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173. The initial administrator email is `admin@sportyshoes.local` and the local demo password is `ChangeMe123!`. Set `SPORTY_ADMIN_PASSWORD` before the first backend startup to choose a different bootstrap password. Afterwards use the Password screen to change it. Customers can create their own accounts. Three sample products are created only when the product table is empty.

The default database is stored in `data/`, which is ignored by Git. `SPORTY_DATABASE_URL` and `SPORTY_DATABASE_PASSWORD` can override its connection. Stop the application before copying the database files for a local backup. Purchases are simulated and do not collect payment.

## Build a single application

```sh
sh scripts/build.sh
java -jar target/SportyShoes-0.0.1-SNAPSHOT.jar
```

The build includes the React assets in the Spring Boot jar. Open http://localhost:8080 to use the packaged application. The script performs a clean build, installs the locked frontend dependencies, builds the frontend and packages the backend with its tests.

## Verification

```sh
mvn test
cd frontend
npm ci
npm test
npm run build
npm audit
```

The backend integration suite exercises real session login, authorization, CSRF, registration, password change, product management, stock deduction including simultaneous attempts to buy the last pair, validation, historical report data, inclusive filters and customer purchase isolation. Frontend tests exercise catalogue/login navigation, registration, product administration, report filters, user search, error display and CSRF transport. GitHub Actions runs these checks and builds the combined jar for pushes and pull requests.

## API and data model

Public reads use `GET /api/products`; `GET /api/csrf` initializes a CSRF token. Registration uses `POST /api/register`. Session login uses form-encoded `POST /api/login` with `username` and `password`; logout uses `POST /api/logout`. Authenticated customers access `GET /api/me`, `POST /api/password`, and `GET` or `POST /api/purchases`.

Administrator endpoints use `/api/admin/products` with GET/POST, `/api/admin/products/{id}` with PUT/DELETE, `/api/admin/users?search=...` and `/api/admin/reports?from=YYYY-MM-DD&to=YYYY-MM-DD&category=...`. DELETE archives products. Mutations require the `X-XSRF-TOKEN` header obtained from `/api/csrf`. JSON prices and purchase totals use decimal euro amounts. Dates use the server's local calendar date; report boundaries are inclusive.

The prototype retains the existing Spring Boot 2 / javax architecture. Deployment considerations and remaining production requirements are explained in the specification. The application is intended for the budget-approval demonstration described in the original brief.
