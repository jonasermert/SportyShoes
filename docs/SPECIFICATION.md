# SportyShoes prototype specification

## Purpose and scope

SportyShoes is a stakeholder demonstration of the sports shoe store described in the original README. A Java Spring Boot server owns authentication, inventory, accounts and purchases. A React application styled with Tailwind provides the customer and administrator interfaces. Checkout records a simulated purchase; payment collection, shipping and refunds are outside this prototype.

## Appearance and interactions

The interface uses a warm neutral background, white product cards, orange accents and a dark campaign banner. Responsive grids adapt the catalogue to narrow screens. Every input has a visible label, keyboard focus is visible, and request errors and progress use accessible live status regions. Tables scroll horizontally on smaller screens.

Visitors browse active shoes, filter by category and see price and stock. Buying a pair prompts an anonymous visitor to sign in. Registration requires a name, unique email and a password of at least eight characters. Authenticated customers can buy a pair and view their own purchases. Purchases are recorded atomically with stock deduction. Sold-out and archived products cannot be bought. Purchase records retain the original name, category and total after a product is edited.

Administrators sign in through the same form and receive additional Products, Users and Reports navigation. They create products, edit their name, category, description, price and stock, and archive or reactivate them. Categories are administrator-defined strings; assigning a new category immediately makes it available in the catalogue filter. Archiving hides a product without deleting purchase history.

The user directory shows name, email, role and registration date. Search matches name or email without case sensitivity. Purchase reports accept an inclusive start date, inclusive end date and exact category. Empty filters include all purchases. The report shows customer, product, category, quantity, purchase date, total and the sum of matching totals. An inverted or invalid date range is rejected.

Both customers and administrators can change their password after verifying the current password. The current session ends and the user signs in with the new password. Administrative requests are protected on the server, independently of navigation visibility. Password hashes never appear in API responses.

## Java concepts used

BaseEntity demonstrates inheritance and a mapped superclass: Account, Product and Purchase share creation and update timestamps populated by JPA lifecycle callbacks. Entity classes model the domain; Spring Data repository interfaces express typed persistence contracts. Constructor injection supplies repositories and password encoding to controllers and startup initialization. Bean validation annotations define request constraints. Streams, lambdas and predicates implement directory and report filters. BigDecimal maintains decimal currency arithmetic. LocalDate implements inclusive date comparisons while Instant records creation timestamps. Transaction boundaries and pessimistic database locking keep concurrent purchases from overselling stock. Exceptions map failed business rules to HTTP status codes. Spring Security provides roles, sessions, BCrypt hashing and CSRF protection.

## Persistence and deployment limits

The default file-based H2 database preserves data across restarts. Tests use an isolated memory database. The administrator bootstrap password is configurable through SPORTY_ADMIN_PASSWORD and applies only when that account is first created. The default credentials exist for a local demonstration. Before hosting publicly, configure a unique administrator password, HTTPS, secure session cookies, a supported database and migrations. This project retains the existing Spring Boot 2 / javax architecture with its latest community 2.7 patch; moving to a currently supported Spring Boot major is a separate production hardening task. No payment gateway or fulfillment service is connected.

Password changes invalidate the current browser session; pre-existing sessions in other browsers are not centrally revoked. Filtering currently reads the small prototype dataset in memory. Pagination, database-backed reporting queries, account recovery and rate limiting are future production requirements.
