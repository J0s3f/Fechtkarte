Code is clean if it can be understood easily – by everyone on the team. Clean code can be read and enhanced by a developer other than its original author. With understandability comes readability, changeability, extensibility and maintainability.

---

## General rules
1. Follow standard conventions.
2. Keep it simple stupid. Simpler is always better. Reduce complexity as much as possible.
3. Boy scout rule. Leave the campground cleaner than you found it.
4. Always find root cause. Always look for the root cause of a problem.

## Design rules
1. Keep configurable data at high levels.
2. Prefer polymorphism to if/else or switch/case.
   ```java
   // Avoid this
   public double calculate(double amount, String paymentType) {
     if ("credit".equals(paymentType)) { // ... }
     else if ("debit".equals(paymentType)) { // ... }
   }
   // Prefer this
   public double calculate(double amount, PaymentMethod paymentMethod) {
     return paymentMethod.calculate(amount);
   }
   ```
3. Separate multi-threading code.
4. Prevent over-configurability.
5. Use dependency injection.
   ```java
   // Avoid this (tight coupling)
   public class MyService {
     private final Database database = new Database();
   }
   // Prefer this (constructor injection)
   public class MyService {
     private final Database database;
     public MyService(Database database) { this.database = database; }
   }
   ```
6. Follow Law of Demeter. A class should know only its direct dependencies.

## Understandability tips
1. Be consistent. If you do something a certain way, do all similar things in the same way.
2. Use explanatory variables.
3. Encapsulate boundary conditions. Boundary conditions are hard to keep track of. Put the processing for them in one place.
   ```java
   // Avoid this
   if (limit > 50) { // ... }
   // Prefer this
   boolean isOverLimit = limit > MAX_LIMIT;
   if (isOverLimit) { // ... }
   ```
4. Prefer dedicated value objects to primitive type.
   ```java
   // Avoid this
   public void book(String customerName, String currency, double amount) {}
   // Prefer this
   public void book(CustomerName customer, Money amount) {}
   ```
5. Avoid logical dependency. Don't write methods which works correctly depending on something else in the same class.
6. Avoid negative conditionals.
   ```java
   // Avoid this
   if (!user.isActive()) {}
   // Prefer this
   if (user.isInactive()) {}
   ```

## Names rules
1. Choose descriptive and unambiguous names.
2. Make meaningful distinction.
3. Use pronounceable names.
4. Use searchable names.
5. Replace magic numbers with named constants.
6. Avoid encodings. Don't append prefixes or type information.

## Functions rules
1. Small.
2. Do one thing.
3. Use descriptive names.
4. Prefer fewer arguments.
5. Have no side effects.
6. Don't use flag arguments. Split method into several independent methods that can be called from the client without the flag.

## Comments rules
1. Always try to explain yourself in code.
2. Don't be redundant.
3. Don't add obvious noise.
4. Don't use closing brace comments.
5. Don't comment out code. Just remove.
6. Use as explanation of intent.
7. Use as clarification of code.
8. Use as warning of consequences.
9. Do not use comments to describe ongoing changes or implementation steps during development. Comments are for permanent explanations of complex logic, design choices, or potential consequences, focusing on *why* rather than *what* is being done.
10. Avoid "What" comments. Do not comment on what the code is doing (e.g., `// returns the user's name` or `// Added this method`). The code itself should clearly express *what* it does. Use comments only to explain *why* a piece of code exists, especially for non-obvious design choices or complex business logic.

## Source code structure
1. Separate concepts vertically.
2. Related code should appear vertically dense.
3. Declare variables close to their usage.
4. Dependent functions should be close.
5. Similar functions should be close.
6. Place functions in the downward direction.
7. Keep lines short.
8. Don't use horizontal alignment.
9. Use white space to associate related things and disassociate weakly related.
10. Don't break indentation.

## Objects and data structures
1. Hide internal structure.
2. Prefer data structures.
3. Avoid hybrids structures (half object and half data).
4. Should be small.
5. Do one thing.
6. Small number of instance variables.
7. Base class should know nothing about their derivatives.
8. Better to have many functions than to pass some code into a function to select a behavior.
9. Prefer non-static methods to static methods. This is most critical for methods that have dependencies or operate on state, as it is key to enabling testability and dependency injection.

## Tests
1. One assert per test. This is better stated as "one logical concept per test." A test method should validate a single, specific behavior. This makes tests easier to read, and failures easier to diagnose, even if it requires multiple assertion statements to fully validate that concept.
2. Readable.
3. Fast.
4. Independent.
5. Repeatable.

## Code smells, never do the following:
1. Rigidity. The software is difficult to change. A small change causes a cascade of subsequent changes.
2. Fragility. The software breaks in many places due to a single change.
3. Immobility. You cannot reuse parts of the code in other projects because of involved risks and high effort.
4. Needless Complexity.
5. Needless Repetition.
6. Opacity. The code is hard to understand.

## Code conventions used in the current codebase
- Standard Kotlin naming conventions (PascalCase for classes, camelCase for methods and variables).
- Modern Kotlin features are used, such as Streams for data manipulation.
- The code is generally well-formatted and readable, although no formal style guide is enforced.

## Code organization and package structure
- The codebase should be organized by feature or layer in a Hexagonal Architecture. For example:
  - `at.j0s.meyercard.app.domain` (core entities and business rules)
  - `at.j0s.meyercard.app.application.port.api` (interfaces for application services, the "inbound" ports)
  - `at.j0s.meyercard.app.application.port.spi` (interfaces for driven adapters, the "outbound" ports e.g., `Database`)
  - `at.j0s.meyercard.app.application.service` (implementations of the application services)
  - `at.j0s.meyercard.app.adapter.persistence` (database implementation of an outbound port)
  - `at.j0s.meyercard.app.adapter.cli` (entry point for user interaction, uses an inbound port)

## Unit and integration testing approaches
- Unit testing should use Junit5 and modern testing practices.

## Development Workflow & Commits

This project follows a Test-Driven Development (TDD) workflow. The goal is to ensure that all new functionality is covered by tests from the outset, leading to a more robust and maintainable design. All changes should be small, logical, and committed frequently.

### TDD Cycle: Red-Green-Refactor

All new features must be implemented using the following cycle:

1.  **Red - Write a Failing Test:**
    *   Before writing any implementation code, write a small, automated test for a single piece of new functionality.
    *   The test must fail because the feature does not yet exist. This verifies that the test is actually testing something and isn't passing by mistake.
    *   It is acceptable to commit at this "Red" stage with a message like `test: Add failing test for [feature]`.

2.  **Green - Make the Test Pass:**
    *   Write the *absolute simplest* production code to make the failing test pass.
    *   Do not add extra features or "gold-plate" the code at this stage. The goal is simply to get the test to pass.

3.  **Refactor - Improve the Code:**
    *   With the safety of a passing test, refactor the code you just wrote to improve its design, remove duplication, and ensure it adheres to all clean code principles.
    *   Re-run all tests to ensure they still pass after refactoring.
    *   The commit for the new feature should be made at this stage, after the code is clean and the test is green. The commit should bundle the test and the implementation.

### General Commit Rules

*   **Verify Before Committing:** Before any commit, the project must build and all tests must pass. A commit must *never* be made if the code does not compile or if any test is failing.
    *   For a small, incremental change within a single TDD red-green-refactor cycle, running the fast/incremental test loop (not a full clean rebuild) is sufficient before committing, as long as it passes. Reserve the full clean build for milestone commits — completing a task, before a PR, or after any change to build configuration, dependencies, or tooling, where the fast loop's assumptions might not hold.
    *   (If the `clean` step fails due to file locks, a non-cleaning build like `mvn install` is an acceptable compromise).
*   **Capture Full Command Output:** When running a build or other command where only part of the output is needed right now (e.g. piping through `| tail`), also redirect the full output to a temp file at the same time (e.g. `command | tee /tmp/output.log | tail -50`). If the fuller output turns out to be needed later — to diagnose a failure the tail didn't show, for instance — it's already on disk instead of requiring a rerun of a possibly long-running command.
*   **Message Formatting:**
    *   Use the imperative mood (e.g., "Add test for..." not "Added test for...").
    *   For a **new file**, the commit message should reflect its creation (e.g., "feat: Create initial test plan").
    *   For a **modification**, the message should describe the change (e.g., "refactor: Improve error handling").

## Documentation Workflow

As an AI agent developing this project, you are required to keep the project documentation up to date.

1.  **`FEATURES.md`:** After successfully implementing a new feature, add an entry to this file describing the feature from a user's perspective.
2.  **Design decisions:** After making a significant design decision (e.g., adding a new library, choosing a specific architectural pattern, or making a non-obvious implementation choice), document the decision *and the reasoning behind it* — not just what was chosen, but what else was considered and why it lost. A decision recorded without its rationale is one the next reader has to re-litigate.
3.  **Status:** After completing a major feature, update the project's status documentation so it reflects what actually ships, not what was planned.
4. **`CLAUDE.md`:** General rules for the AI Agent. Modify if the user gives new general guidelines.
5. **Changelogs, every version bump, both locations:** Whenever `app/build.gradle.kts`'s `versionCode`/`versionName` is bumped, add a changelog entry for that release in *both* places, kept content-identical:
   - `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` — filename is the new `versionCode`.
   - `distribution/play-store/release-notes/<versionName>.txt` — filename is the new `versionName`.

   Write it from the user's perspective (what changed, not which files), one bullet per change, "No user-facing changes" for a pure maintenance release. Do this as part of the same commit that bumps the version — a version bump with no changelog entry is an incomplete release, the same way a feature without a `FEATURES.md` entry is.

