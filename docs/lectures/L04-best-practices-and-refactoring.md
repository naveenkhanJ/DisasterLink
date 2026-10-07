# SE3070 — Case Studies in Software Engineering — Week 4

## Chapter 4: Some Best Practices & Refactoring Techniques

> ⚠️ **Flag (scope and exclusions):** Course-administration slides (title slide, contents list, acknowledgments, closing contact slide) are left out because they carry no teachable content. So are the decorative pictures (the light-bulb mascot, the closing photograph) and the computer-generated "messy desk / tidy desk" cartoon on the refactoring definition slide. The two pictures that do carry a point (a four-panel joke about not commenting code, and a chart of good versus poor design over time) are expressed in words in Sections 4.1 and 4.9. The slides' reference list is kept (see "Further reading" at the end).

> ⚠️ **Flag (no past papers, no tutorials):** No past exam paper exists in the project's knowledge base, and this module has no tutorial sheets. The "Possible Exam Angles" questions are therefore calibrated from two things only: the quiz brief for the CSSE lab quiz (multiple-choice questions on code snippets: identify code smells, choose the best refactoring technique, identify SOLID violations, and say how to fix them) and the style of the Lab Sheet 3 tasks.

> ⚠️ **Flag (numbering assumptions):** The slides do not state a lecture number. "Lecture 4" comes from you, not from the slides, and "Week 4" is assumed to equal the lecture number. Only the chapter number and filename depend on this.

> ⚠️ **Flag (lab sheet split across two chapters):** Lab Sheet 3 (its own heading says "Practical Sheet 04") covers two lectures. Part A (Tasks 1–10, coding standards and best practices) belongs to this chapter and is answered in Sections 4.1 to 4.7. Part B (SOLID) and Part C (code smells) belong to Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells). The lecture and the lab sheet line up topically, so no mismatch exists. The lab sheet's exception-handling hint even says to follow "the example on the lecture slide", which is this lecture's exception-handling example.

> ⚠️ **Flag (lab-only content):** Two Part A tasks need more than the slides teach. Task 6 (type-safe collections) is not on any slide; it comes from the lab sheet's linked article, so Section 4.6 (Type-Safe Collections with Generics) is written from the lab sheet and that article. Task 8 (Javadoc) is only hinted at on the slides ("documentation and comments"); its detail comes from the lab sheet's linked article in Section 4.1 (Code Quality, Coding Standards and Naming Conventions).

**What you should be able to do after this chapter:** explain how coding standards, best practices and code quality relate; apply best practices to improve maintainability and readability; identify code smells in a codebase and propose suitable refactoring techniques; and use refactoring to raise cohesion, reduce coupling and move code towards the SOLID principles.

**How the lab answers are shown.** Lab Sheet 3 asks you to improve a provided Java project called `BadPracticeEmployeeSystem`. The answers below use short before/after excerpts from that project. Every "after" excerpt comes from a refactored version that was compiled with all compiler warnings enabled and run. Where the refactored code behaves differently from the original, the difference is stated.

**Where each Part A task is answered:**

| Task | Topic | Section |
|---|---|---|
| 1 | Package structure and naming conventions | 4.1 (Code Quality, Coding Standards and Naming Conventions) |
| 2 | Logging practices | 4.2 (Logging) |
| 3 | Exception handling | 4.3 (Exception Handling) |
| 4 | Use of constants | 4.4 (Constants and External Configuration) |
| 5 | External configuration via a properties file | 4.4 (Constants and External Configuration) |
| 6 | Collection type safety | 4.6 (Type-Safe Collections with Generics) |
| 7 | Deprecated API usage | 4.5 (Avoiding Deprecated APIs) |
| 8 | Code comments and Javadoc | 4.1 (Code Quality, Coding Standards and Naming Conventions) |
| 9 | Cohesion and separation of concerns | 4.7 (Organizing Code Effectively) |
| 10 | Utility class refactoring | 4.7 (Organizing Code Effectively) |

---

### 4.1 Code Quality, Coding Standards and Naming Conventions

**Why code quality matters.** Poor code quality leads to *technical debt*: the extra future cost of quick or messy solutions, which must eventually be paid back as extra effort whenever the code has to be understood, changed or fixed. Design quality is not the same as implementation quality: even a great design can be ruined by bad code. Good code is easier to understand, modify and extend without breaking existing features.

**What coding standards are.** A coding standard is a set of agreed conventions that make all the code in a project look and behave uniformly. Four areas are covered:

1. **Naming conventions:** consistent names for classes, methods, variables, constants and so on.
2. **Package or module structure** that matches the architecture of the system.
3. **Consistent formatting:** indentation, braces, whitespace and line wrapping.
4. **Documentation and comments:** explain *why* the code does something, not just *what* it does (the code already shows what).

The point of the four-panel joke about commenting is that "I'll remember what this code does, I wrote it myself, nobody else will work on it, so I don't need comments" is wrong on every step: memory fades, other people inherit code, and an uncommented decision has to be re-discovered from scratch.

Published style guides exist for the common languages, and teams often adopt one instead of inventing their own: the Google C++ Style Guide, the Google Java Style Guide and the Google Python Style Guide. A second joke on the same theme (explaining your variable-naming scheme to your colleagues with a wall of string and photographs) makes the point that names should be understandable without an explanation.

**Java naming conventions (Oracle).** The lab sheet points to Oracle's Java naming conventions:

| Element | Convention | Example |
|---|---|---|
| Package | All lower case; the prefix of a unique name is a reversed domain name such as `com` | `com.sliit.employeesystem.service` |
| Class | Noun; each word capitalised (`UpperCamelCase`); simple and descriptive | `EmployeeService` |
| Interface | Named like a class | `Notifier` |
| Method | Verb; first word lower case, later words capitalised (`lowerCamelCase`) | `getName()`, `loadEmployees()` |
| Variable | `lowerCamelCase`; meaningful; not starting with `_` or `$`; one-letter names only for short throw-away loop variables | `nickname`, `i` |
| Constant | All upper case, words separated by underscores (`UPPER_SNAKE_CASE`) | `MINIMUM_SALARY` |

> ⚠️ **Flag (age of the naming-conventions page):** The Oracle page linked by the lab sheet carries Oracle's banner "for archive purposes only" and "not being actively maintained", and was last revised on 20 April 1999; its naming rules (which are the ones in the table above) are still the standard Java conventions. The link as printed in the lab sheet is broken across a line and reads `…codeconventions-` then `namingconventions.html`; the working address has the hyphen kept.

**Lab Sheet 3, Task 1 — Package structure and naming conventions.** *Ensure all classes are in appropriate packages (consider a layered architecture) and follow standard Java naming conventions for classes, methods, variables and constants.*

*Packages.* The original packages were named by accident (`lib`, `utils`, `repo`, `notify`, `services`), and several classes sat in the root package. The refactored project uses a layered structure, where each package holds one kind of class and dependencies point downwards (upper layers use lower ones, never the reverse):

| Package | Layer | Contents |
|---|---|---|
| `app` | Entry point | `Main` |
| `service` | Application logic | `EmployeeService` |
| `contracts` | Small role interfaces | `EmployeeStorage`, `EmployeeExporter`, `PayslipPrinter`, `PromotionNotifier`, `OrgChartRenderer` |
| `notification` | Outbound messaging | `Notifier` (interface), `EmailNotifier` |
| `repository` | Data access | `EmployeeRepository` (interface), `InMemoryEmployeeRepository` |
| `model` | Domain objects | `Employee`, `ContractEmployee`, `Department`, `DepartmentLevel`, `Address`, `EmploymentStatus` |
| `util` | Static helpers | `LoggerFactory`, `NameFormatter`, `DateTimeUtil`, `PropertiesLoader` |

The dependency direction is `app` → `service` → (`repository`, `notification`, `contracts`) → `model`, with `util` used by all. There are no cycles between packages (checked by reading every `import`).

*Names.* Examples of what changed:

| Original | Problem | Refactored |
|---|---|---|
| `getname()` / `setname(String n)` | Method names not in `lowerCamelCase`; one-letter parameter | `getName()` / `setName(String name)` |
| `public String sNick;` | Hungarian-style prefix (`s` for String); public field | `private String nickname;` with `getNickname()` / `setNickname(...)` |
| `isActive`, `isOnLeave`, `isTemp` (three booleans) | Flag soup that allows contradictory states | `EmploymentStatus` enum and `isTemporary()` |
| `DateTimeBadUtil` | The class name describes its quality, not its purpose | `DateTimeUtil` |
| `HashUtil.normalizeName` | The class contained no hashing, so the name misled | Deleted; logic moved to `NameFormatter.format` |
| `Department.code` (an `int` 1, 2 or 3) | Name says nothing about meaning | `DepartmentLevel` enum with `HEADQUARTERS`, `REGIONAL`, `FIELD` |
| package `notify` | A verb; a package holds a group of related classes, so a noun that names the group reads better | package `notification` |

**Lab Sheet 3, Task 8 — Code comments and Javadoc.** *Add meaningful Javadoc to classes and methods, and inline comments where necessary.*

*What Javadoc is.* Javadoc is Java's built-in documentation system. A *documentation comment* starts with `/**` and ends with `*/`, and is placed directly before a class, field or method. The `javadoc` tool reads these comments and generates HTML documentation of the public API. A Javadoc comment has a main description (what the element is or does) followed by *block tags*, which start with `@` and each sit on their own line:

| Tag | Meaning |
|---|---|
| `@param name description` | Describes one parameter of a method |
| `@return description` | Describes the returned value |
| `@throws ExceptionType description` | Says when an exception is thrown |
| `@see`, `@since`, `@author`, `@version`, `@deprecated` | References, version and authorship notes, and a deprecation notice |

Inside a description, `{@code text}` shows text as code and `{@link Class}` makes a link to another class.

*A meaningful Javadoc comment* says what the element does and any promises or restrictions, not just restates its name. From the refactored project:

```java
/**
 * Sets the salary. A negative value is replaced by {@value #MINIMUM_SALARY} and a warning is
 * logged (the original printed to {@code System.out}/{@code System.err} instead).
 *
 * @param salary the new salary
 */
public void setSalary(double salary) {
    if (salary < MINIMUM_SALARY) {
        LOG.warning("Negative salary is not allowed; using the minimum salary instead.");
        this.salary = MINIMUM_SALARY;
        return;
    }
    this.salary = salary;
}
```

*Implementation Analysis.* The description states the rule (negative values are clamped) that a reader cannot see from the signature. `@param` names what the value means. The inline comments in this project follow the "why, not what" rule; for example, `Locale.ROOT` in `NameFormatter` carries a comment explaining that it keeps the result independent of the machine's language settings, which is something the code alone does not reveal.

*A weak comment for contrast:* `// increment i` above `i++` repeats the code; `// skip the header row` above `i = 1` explains the reason. Only the second is worth writing.

*Result.* Every class and every public method that is not an override (an overriding method inherits its documentation) in the refactored project has a Javadoc comment (55 such types and methods were checked). The `javadoc` tool with all documentation checks enabled (`-Xdoclint:all`) reports zero warnings and zero errors on the public API.

**Possible Exam Angles (with model answers)**

**Q1:** Which of these names violate Java conventions, and what should each be? `class employee_service`, `int Total_Amount`, `static final double taxRate = 0.12`, `void GetName()`, `package com.Acme.Utils`.

**A1:** `employee_service` → `EmployeeService` (class names are `UpperCamelCase`). `Total_Amount` → `totalAmount` (variables are `lowerCamelCase`). `taxRate` → `TAX_RATE` (a `static final` constant is `UPPER_SNAKE_CASE`). `GetName` → `getName` (methods start with a lower-case verb). `com.Acme.Utils` → `com.acme.utils` (packages are all lower case).

**Q2:** Explain the guideline "comments should explain why, not just what", with an example.

**A2:** The code already shows *what* it does, so a comment that repeats it (`// add 1 to count`) adds nothing and can go stale. A useful comment records the reason or a hidden constraint, for example `// Locale.ROOT: keeps the result independent of the machine's language settings`. Future readers cannot recover that reasoning from the code alone.

**Q3:** List the four areas a coding standard covers.

**A3:** Naming conventions; package or module structure that matches the architecture; consistent formatting (indentation, braces, whitespace, line wrapping); and documentation and comments.

**Q4:** Write a correct Javadoc comment for `double average(int[] values)` that throws `IllegalArgumentException` when the array is empty.

**A4:**
```java
/**
 * Calculates the arithmetic mean of the given values.
 *
 * @param values the values to average; must contain at least one element
 * @return the mean of {@code values}
 * @throws IllegalArgumentException if {@code values} is empty
 */
double average(int[] values) { /* ... */ }
```

---

### 4.2 Logging

**The practice.** Use a logging framework instead of printing to the console. Choose an appropriate *log level* for every message and include enough context (for example the identifier of the thing involved) to make the message useful when read later.

**Why logging beats printing.** A logging framework produces structured, levelled, timestamped records that are useful for troubleshooting. Its output is configurable per environment (for example, detailed messages while developing and only warnings and errors in production, with no code change). It also supports aggregation and monitoring, meaning that log records from many programs can be collected in one place, searched and displayed; a well-known example is the *ELK Stack* (Elasticsearch, Logstash and Kibana, tools for collecting, searching and visualising logs).

> ⚠️ **Flag (ELK is only named):** The slide names the ELK Stack with a link but does not explain it. The description in parentheses is the standard expansion of the name and was not checked against the linked page.

**Bad versus good, as printed.**

```java
// Bad practice
System.out.println("User login failed for id=" + userId);

// Good practice
logger.warn("User login failed for id={}", userId);
```

*Implementation Analysis.* The bad version always prints to standard output, has no severity and no timestamp, and cannot be switched off or redirected without editing the code. The good version records the event at the *warning* level, and `{}` is a placeholder that is filled with `userId` only if the message is actually written, which avoids building a string that nobody reads.

> ⚠️ **Flag (the example uses a different logging library from the lab):** `logger.warn(...)` with `{}` placeholders is the style of the SLF4J and Log4j libraries. The lab project uses Java's built-in `java.util.logging`, where the method is `warning(...)` (there is no `warn`) and the placeholder is `{0}`. This was checked by compiling and running: `logger.warn(...)` on a `java.util.logging.Logger` does not compile, and a `{}` in a `java.util.logging` message is printed literally. In the lab, write `LOG.log(Level.WARNING, "User login failed for id={0}", userId)`.

**Logging with `java.util.logging`.** The second logging example uses the built-in Java logger:

```java
// ❌ Do not do this
System.out.println(employee.toString() + "\n");

/** Initialize logger */
public static final Logger log = Logger.getLogger(AbstractService.class.getName());

log.info(employee.toString() + "\n");
```

*Implementation Analysis.* A logger is created once per class (`static final`), and it is named after the class (`AbstractService.class.getName()`), so every record shows which class produced it. `log.info(...)` then replaces the console print. The red cross marks the `System.out.println` line as the practice to remove.

**Log levels.** `java.util.logging` has seven levels, each with a numeric value. A message is written only if its level is at or above the logger's configured level. The values below were confirmed by running Java:

| Level | Value | Typical use |
|---|---|---|
| `SEVERE` | 1000 | A serious failure: something that stops an operation from working |
| `WARNING` | 900 | A potential problem or an unexpected but survivable situation |
| `INFO` | 800 | Normal, noteworthy events (start-up, a request completed) |
| `CONFIG` | 700 | Configuration details |
| `FINE`, `FINER`, `FINEST` | 500, 400, 300 | Increasingly detailed tracing for developers |

The lab sheet asks for three of these: `INFO`, `WARNING` and `SEVERE`.

**Logging inside exception handlers.** Five catch blocks that all handle the exception the same way:

```java
} catch (NumberFormatException e) {
    log.log(Level.SEVERE, e.getMessage());
} catch (XPathExpressionException e) {
    log.log(Level.SEVERE, e.getMessage());
} catch (SAXException e) {
    log.log(Level.SEVERE, e.getMessage());
} catch (IOException e) {
    log.log(Level.SEVERE, e.getMessage());
} catch (ParserConfigurationException e) {
    log.log(Level.SEVERE, e.getMessage());
}
```

*Implementation Analysis.* Each failure is recorded at `SEVERE` with its message, which is much better than ignoring it (see Section 4.3, Exception Handling). Because all five handlers are identical, Java lets you combine them into one multi-catch, `catch (NumberFormatException | XPathExpressionException | SAXException | IOException | ParserConfigurationException e)`, as long as none of the listed types is a subclass of another. Logging only `e.getMessage()` loses the stack trace; passing the exception itself, `log.log(Level.SEVERE, "message", e)`, keeps it.

**Lab Sheet 3, Task 2 — Logging practices.** *Replace all `System.out.println` calls with a proper logging framework and use correct log levels (INFO, WARNING, SEVERE). Hint: the logger is already implemented in `Utils`.*

The provided `LoggerFactory` (in the utility package) wraps `java.util.logging`. Every `System.out`/`System.err` call in the project was replaced by a logger obtained from it:

```java
private static final Logger LOG = LoggerFactory.getLogger(EmployeeService.class);
LOG.log(Level.INFO, "EMP: {0}", NameFormatter.format(employee.getName()));
```

Levels used, and why:

| Where | Message | Level |
|---|---|---|
| `Main` | Environment, department, year, month, employees loaded | `INFO` (normal events) |
| `InMemoryEmployeeRepository` | "Repository: loading employees" | `INFO` |
| `EmailNotifier` | "Email sent to …" | `INFO` |
| `Employee.setSalary` | Negative salary was clamped to the minimum | `WARNING` (unexpected but survivable) |
| `PropertiesLoader` | The settings file could not be read | `SEVERE` for the technical cause, `WARNING` for the plain-language "defaults will be used" message |

**A bug in the provided `LoggerFactory`.** As supplied, `getLogger` attached its own console handler *and* left `setUseParentHandlers(true)`, so every message was printed twice (once by its handler and once by the root logger's console handler). This was confirmed by running it. The fix:

```java
// BEFORE (excerpt)
ConsoleHandler handler = new ConsoleHandler();
handler.setLevel(Level.ALL);
logger.addHandler(handler);
// ...
logger.setUseParentHandlers(true);

// AFTER (excerpt)
ConsoleHandler handler = new ConsoleHandler();
handler.setLevel(Level.ALL);
handler.setFormatter(new CompactFormatter());
logger.addHandler(handler);
logger.setUseParentHandlers(false);
```

*Implementation Analysis.* Turning off the parent handlers means each record is written once. The custom formatter prints one line in the form `[LEVEL] ClassName - message`. The check-then-add step is wrapped in a `synchronized` block, so two threads asking for the same logger at the same moment cannot attach two handlers.

**Possible Exam Angles (with model answers)**

**Q1:** Give three reasons a logging framework is preferable to `System.out.println` in a real application.

**A1:** It gives structured records with a level and a timestamp for troubleshooting; its output can be configured per environment without editing code (for example, fewer messages in production); and its records can be aggregated and monitored by tools such as the ELK Stack.

**Q2:** Choose the level: (a) a required configuration file is missing and the application cannot start; (b) a user is created successfully; (c) a retry succeeded after the first attempt timed out.

**A2:** (a) `SEVERE`: a serious failure. (b) `INFO`: a normal, noteworthy event. (c) `WARNING`: an unexpected but survivable situation.

**Q3:** What is wrong with this line, and how do you correct it (using `java.util.logging`)? `System.out.println("Payment failed for " + orderId);`

**A3:** It prints to the console with no level, no timestamp and no way to configure or redirect it, and it hides the class it came from. Correct it with a per-class logger: `LOG.log(Level.SEVERE, "Payment failed for {0}", orderId);` (or `SEVERE`/`WARNING` as appropriate).

**Q4:** A team's logs show every message twice. What is a likely cause in `java.util.logging`?

**A4:** The logger has its own console handler and also forwards each record to its parent handlers (the root logger's console handler), so the message is printed twice. Setting `logger.setUseParentHandlers(false)` fixes it.

---

### 4.3 Exception Handling

**The practice.** Do not swallow exceptions (catch them and do nothing), and use meaningful messages. Do not just throw exceptions upwards either: handle them properly.

**Bad versus good, as printed.** In the bad version the exception is caught and ignored; in the good version a specific exception is handled in three steps.

```java
// Not good
try {
    process(order);
} catch (Exception e) {
    // ignore
}

// Good
try {
    process(order);
} catch (PaymentException e) {
    log.error("Payment failed for orderId={}", order.getId(), e);
    notifyUser(order.getCustomer(), "Payment could not be processed.");
    rollbackTransaction(order);
    // handled gracefully without crashing the system
}
```

*Implementation Analysis.* The bad version has two faults. `catch (Exception e)` is too broad: it catches every kind of failure, including ones the code never anticipated, and treats them identically. An empty handler then discards the failure, so the program carries on as if nothing happened and nobody can tell that something failed (called *swallowing* the exception). The good version catches one specific exception type, `PaymentException`, that the code knows how to deal with. Its handler does three things, in this order:

1. **Log the error properly** (with the exception attached so the stack trace is kept).
2. **Notify the user** of the unexpected situation and what will happen next, in a short, plain-language message (not a stack trace).
3. **Roll back** any changes made during the process that failed, so no half-finished state is left behind.

`PaymentException`, `notifyUser` and `rollbackTransaction` are illustrative names; they do not exist in the lab project or in Java.

> ⚠️ **Flag (the good example's logging call is SLF4J-style):** `log.error(..., order.getId(), e)` with a `{}` placeholder and the exception as the last argument is SLF4J or Log4j style (see Section 4.2, Logging). With `java.util.logging` the equivalent is `LOG.log(Level.SEVERE, "Payment failed for orderId=" + order.getId(), e);`, where the exception is the last argument so the stack trace is kept.

**Try-with-resources.** The lab sheet also asks for try-with-resources for file and resource handling. A *resource* here is an object that must be closed after use (a file stream, a database connection). In a *try-with-resources* statement the resources are declared in parentheses after `try`; Java closes them automatically when the block ends, whether it ends normally or by an exception, and closes them in the reverse of the order they were opened. A class can be used this way if it implements `AutoCloseable`. This was confirmed by running code that opened two resources and then threw: the output showed both were closed, the second one first, before the exception was caught.

The configuration loader shown in Section 4.4 (Constants and External Configuration) uses the older way: the stream is declared before the `try`, and a `finally` block closes it, itself wrapped in another `try/catch`. Try-with-resources removes all of that:

```java
// Older pattern: manual close in finally
FileInputStream fis = null;
try {
    fis = new FileInputStream(CONFIG_FILEPATH);
    props.load(fis);
    return props.getProperty(key);
} catch (IOException e) {
    System.err.println("Could not load configuration: " + e.getMessage());
    return null; // or take a fallback action
} finally {
    if (fis != null) {
        try { fis.close(); } catch (IOException e) { /* log or ignore */ }
    }
}
```

```java
// Try-with-resources (the same method body, rewritten)
try (FileInputStream fis = new FileInputStream(CONFIG_FILEPATH)) {
    props.load(fis);
    return props.getProperty(key);
} catch (IOException e) {
    LOG.log(Level.SEVERE, "Could not load configuration", e);
    return null; // or take a fallback action
}
```

*Implementation Analysis.* The second version cannot forget to close the stream, needs no `null` check, and cannot lose the original exception if closing also fails (the failure from closing is attached to it as a *suppressed* exception, which was confirmed by running an example). The older version also prints to `System.err` (see Section 4.2, Logging), which is why the second version logs instead.

**Lab Sheet 3, Task 3 — Exception handling.** *Replace generic exception handling with specific exceptions, and use try-with-resources for file and resource handling.* The provided `PropertiesLoader` had every fault of the swallowed-exception example above, and more:

```java
// BEFORE
public Map loadAll() {
    Map m = new HashMap(); // raw map
    try {
        java.io.FileInputStream fis = new java.io.FileInputStream("config/config.properties"); // no TWR
        java.util.Properties p = new java.util.Properties();
        p.load(fis);
        for (Object k : p.keySet()) m.put(k, p.get(k));
        fis.close();
    } catch (Exception e) { /* ignore */ } // generic catch
    return m;
}
```

Faults: `catch (Exception e)` is too broad; the empty handler swallows the failure, so a missing file just gives an empty map with no clue why; the stream is closed by hand, so if `load` throws, `close` is never reached and the file handle leaks; and the map is raw (a collection used without stating what it holds; see Section 4.6, Type-Safe Collections with Generics).

```java
// AFTER
public static Map<String, String> loadAll() {
    Properties properties = new Properties();
    try (InputStream in = PropertiesLoader.class.getResourceAsStream(CONFIG_RESOURCE)) {
        if (in == null) {
            throw new IOException("Resource not found on classpath: " + CONFIG_RESOURCE);
        }
        properties.load(in);
    } catch (IOException | IllegalArgumentException e) {
        // IllegalArgumentException: Properties.load rejects malformed \\uXXXX escapes.
        LOG.log(Level.SEVERE, "Could not load configuration from " + CONFIG_RESOURCE, e);
        LOG.warning("The settings file could not be read, so built-in default settings will be used.");
        return Collections.emptyMap(); // "rollback": no partially loaded values escape
    }
    Map<String, String> values = new HashMap<>();
    for (String key : properties.stringPropertyNames()) {
        values.put(key, properties.getProperty(key));
    }
    return Collections.unmodifiableMap(values);
}
```

*Implementation Analysis.* The three-step rule is applied: (1) the technical cause is logged at `SEVERE` with the exception attached; (2) a short plain-language `WARNING` tells the reader what will happen next (built-in defaults are used); (3) on failure an empty map is returned and nothing read so far leaks out, so callers never see a half-loaded configuration. The two exception types are specific (`IOException` for reading problems, `IllegalArgumentException` because `Properties.load` throws it for malformed escape sequences), instead of `Exception`. In the same task the never-used `Employee.clone()` method, which wrapped a checked exception in a bare `RuntimeException`, was deleted as dead code.

**Possible Exam Angles (with model answers)**

**Q1:** Identify every problem in this code and fix it.
```java
FileReader fr = new FileReader("data.txt");
try {
    int c = fr.read();
} catch (Exception e) {
}
fr.close();
```

**A1:** (1) `catch (Exception e)` is too broad; catch `IOException`. (2) The empty handler swallows the failure; log it, tell the user, and undo partial work if needed. (3) `fr.close()` is outside any protection and is skipped if `read()` throws, leaking the file; and the constructor itself throws `FileNotFoundException` and is outside the `try`. Fix: `try (FileReader fr = new FileReader("data.txt")) { int c = fr.read(); } catch (IOException e) { LOG.log(Level.SEVERE, "Could not read data.txt", e); /* tell the user, roll back if needed */ }`.

**Q2:** Name the three things a good exception handler should do.

**A2:** Log the error properly (with the exception), notify the user in a short plain-language message about what happened and what will happen next, and roll back any changes made by the process that failed.

**Q3:** What does try-with-resources guarantee, and what must the resource class implement?

**A3:** It guarantees the resource is closed when the block ends, whether normally or by an exception, in reverse order of opening, without a manual `finally`. The resource class must implement `AutoCloseable`.

**Q4:** Why is `catch (Exception e)` considered poor practice when the code only expects file-reading problems?

**A4:** It also catches unrelated, unexpected failures (such as programming errors) and handles them in the same way, which hides bugs. Catching the specific exception the code can actually handle (`IOException`) means other failures still surface.

---

### 4.4 Constants and External Configuration

**The practice.** Replace *magic numbers* with named constants, and keep settings that vary by environment (addresses, keys, rates that change) in properties files or environment variables instead of in the code.

**Bad, as printed.**

```java
double total = amount + amount * 0.12; // What is 0.12?
String url = "http://dev.api.local:8080"; // hard-coded env
```

**Why this is bad.** A *magic number* is an arbitrary number with no visible meaning (`0.12` could be a tax rate, a discount or a commission). Hard-coded values also scatter configuration across the code, which makes updates error-prone (the same value must be found and changed in many places) and prevents environment-specific overrides (the development address `dev.api.local` cannot be replaced by a production address without editing and recompiling).

**Constants, and a better use of them, as printed.**

```java
static final double VAT_RATE = 0.12;
static final String PAYMENT_BASEURL_KEY = "services.payment.baseUrl";
static final String CONFIG_FILEPATH = "Src\\Config.properties";

double total = amount + amount * VAT_RATE;
String paymentBaseUrl = loadConfigValue(PAYMENT_BASEURL_KEY);
```

*Implementation Analysis.* `static final` makes a constant: one copy for the whole class (`static`) that can never be reassigned (`final`). Its name (`VAT_RATE`) documents the meaning, and it is defined once, so changing the rate is a one-line edit. The second constant does not hold a value but the *key* under which the value is stored in a file, so the code says which setting it wants and the file supplies it.

**The properties file and the loader, as printed.** A properties file is a plain text file of `key=value` pairs:

```properties
services.payment.baseUrl=https://api.payments.com
services.tax.country=LK
```

```java
public static String loadConfigValue(String key) {
    Properties props = new Properties();
    FileInputStream fis = null;
    try {
        fis = new FileInputStream(CONFIG_FILEPATH);
        props.load(fis);
        return props.getProperty(key);
    } catch (IOException e) {
        System.err.println("Could not load configuration: " +
                           e.getMessage());
        return null; // or take a fallback action
    } finally {
        if (fis != null) {
            try { fis.close(); } catch (IOException e) {
                /* log or ignore */ }
        }
    }
}
```

*Implementation Analysis.* `Properties` is Java's class for key/value settings; `load` reads the file; `getProperty(key)` returns the value for one key. Changing the address or the country now means editing the text file, with no recompilation, and a different file can be supplied for each environment. Environment variables serve the same purpose for values that should not be stored in files (for example, `System.getenv("NAME")` reads one).

> ⚠️ **Flag (three weaknesses in the printed loader):** (1) `"Src\\Config.properties"` uses a Windows-style backslash separator, so it only works on Windows; running the same code on Linux fails with `FileNotFoundException` and, because the handler returns `null`, the caller silently receives `null` (this was confirmed by running it). (2) The file is opened again on every call to `loadConfigValue`. (3) The `services.tax.country` key is defined but never used in the slide's code. The loader also uses the older manual-close pattern that Section 4.3 (Exception Handling) replaces with try-with-resources.

**Lab Sheet 3, Task 4 — Use of constants.** *Replace magic numbers and strings with meaningful constants.* The refactored project names every such value:

| Original | Refactored |
|---|---|
| `if (s < 0) { ... s = 0; }` in `Employee.setSalary` (bare `0`) | `private static final double MINIMUM_SALARY = 0.0;` |
| `"Report generated"` inside a method | `private static final String REPORT_DONE_MESSAGE = "Report generated";` |
| `String.format("E%03d", i + 1)` | `private static final String EMPLOYEE_ID_FORMAT = "E%03d";` |
| `"Alice"`, `"Bob"` inside the repository | `FIRST_EMPLOYEE_NAME`, `SECOND_EMPLOYEE_NAME` |
| the path `"config/config.properties"` inside the loader | `private static final String CONFIG_RESOURCE = "/config/config.properties";` |
| `public String region = "LK";` (a hard-coded default in `Department`) | `DEFAULT_COUNTRY = "LK"` used only as a fallback, with the real value read from the configuration file (Task 5) |
| department codes `1`, `2`, `3` (with two `switch`/`if` chains testing them) | `DepartmentLevel` enum with constants `HEADQUARTERS`, `REGIONAL`, `FIELD` |

Constants that have no original counterpart were introduced together with new code: `DEFAULT_ZONE` (`ZoneId.of("Asia/Colombo")`) in `DateTimeUtil`, `KEY_ENVIRONMENT` (`"env"`) and `KEY_COUNTRY` (`"country"`) in `PropertiesLoader`, and `HEAD_OFFICE_NAME` and `DEFAULT_ENVIRONMENT` in `Main`.

*Implementation Analysis.* The last row is the strongest fix: three related numeric codes became an enum, so the compiler rejects an invalid level and the two duplicated conditional chains disappeared (Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells) covers that as the Switch/If-Else Chains smell). All constants follow the `UPPER_SNAKE_CASE` convention from Section 4.1 (Code Quality, Coding Standards and Naming Conventions).

**Lab Sheet 3, Task 5 — External configuration via a properties file.** *Load configuration values from `config.properties` instead of hard-coded values.* The project's `src/main/resources/config/config.properties` already contained `env=dev` and `country=LK`, but nothing read it: the `PropertiesLoader` was never called, and `Department` hard-coded its region as `"LK"`.

```java
// BEFORE: the loader looked for the file relative to the current directory ("config/config.properties")
FileInputStream fis = new java.io.FileInputStream("config/config.properties");
```

```java
// AFTER: the file is read from the classpath, and Main uses the values
private static final String CONFIG_RESOURCE = "/config/config.properties";
// in PropertiesLoader.loadAll():
try (InputStream in = PropertiesLoader.class.getResourceAsStream(CONFIG_RESOURCE)) { /* ... */ }

// in Main.main():
Map<String, String> config = PropertiesLoader.loadAll();
String environment = config.getOrDefault(PropertiesLoader.KEY_ENVIRONMENT, DEFAULT_ENVIRONMENT);
String country = config.getOrDefault(PropertiesLoader.KEY_COUNTRY, DEFAULT_COUNTRY);
Department department = new Department(HEAD_OFFICE_NAME, DepartmentLevel.HEADQUARTERS, country);
```

*Implementation Analysis.* The *classpath* is the set of locations Java searches for compiled classes and resource files; a file under `src/main/resources` is copied onto it when the project is built. Reading from the classpath works from any working directory, unlike the original path, which only worked if the program was started from a folder that happened to contain `config/config.properties` (in the project's layout it did not, so the original loader would have returned an empty map). `getOrDefault` supplies a built-in value when a key is absent, so a missing file degrades to safe defaults instead of stopping the program. This was checked by running the program from a jar file in a different directory and with the resource removed.

**Possible Exam Angles (with model answers)**

**Q1:** Refactor to remove the smells: `if (age >= 18 && score > 65) grant();`. Which best practice does it break, and what does the fix look like?

**A1:** It uses magic numbers (`18`, `65`). Name them: `private static final int ADULT_AGE = 18; private static final int PASS_MARK = 65;` and write `if (age >= ADULT_AGE && score > PASS_MARK) grant();`. If the values differ between deployments, read them from a properties file.

**Q2:** Give two reasons hard-coding `"http://dev.api.local:8080"` in source code is a problem.

**A2:** It scatters configuration through the code, so a change must be found and made in many places, which is error-prone; and it prevents environment-specific overrides, because moving to test or production would mean editing and recompiling the code. Store it in a properties file or environment variable instead.

**Q3:** What is the difference between `static final double VAT_RATE = 0.12;` and `double vatRate = 0.12;` inside a method?

**A3:** The first is a class-level constant: shared by all instances, never reassignable, named in `UPPER_SNAKE_CASE`, and defined once. The second is a local variable created on each call that could be changed and is not visible to other methods, so the same value tends to be re-typed elsewhere.

**Q4:** Why does reading a properties file from the classpath work better than `new FileInputStream("config/config.properties")`?

**A4:** A relative file path depends on the directory the program is started from, so it breaks when started elsewhere. A classpath resource is found wherever the program is run, because the file is packaged with the compiled program.

---

### 4.5 Avoiding Deprecated APIs

**The practice.** Always prefer supported alternatives to deprecated APIs. A *deprecated* API element is one its authors have marked as discouraged: it still works today, but a better replacement exists and it may be removed in a future Java version. The compiler reports it, and its documentation says what to use instead.

**The example, as printed.**

```java
Date d = new Date();
int year = d.getYear(); // deprecated
```

The compiler responds with two notes: "Main.java uses or overrides a deprecated API." and "Recompile with -Xlint:deprecation for details." The program prints `125`. The documentation of `getYear()` says: "Deprecated. As of JDK version 1.1, replaced by `Calendar.get(Calendar.YEAR) - 1900`."

The supported alternative:

```java
LocalDate now = LocalDate.now(ZoneId.of("Asia/Colombo"));
int year = now.getYear();
```

which prints `2025`.

*Implementation Analysis.* `Date.getYear()` does not return the year: it returns the year *minus 1900* (so 2025 gives 125), which is why the documented replacement subtracts 1900. `LocalDate` belongs to the modern `java.time` package, which replaced the old `Date` and `Calendar` classes; `LocalDate.now(ZoneId.of("Asia/Colombo"))` gives today's date in a named time zone, and `getYear()` returns the real calendar year. The compiler note explains how to see the details: recompile with `-Xlint:deprecation`.

> ⚠️ **Flag (the two outputs are different numbers):** The printed outputs, `125` and `2025`, are not the same value. The "replacement" is therefore not behaviour-identical; it is the *correct* value, and the deprecated call was returning something surprising. (A literal behaviour-preserving replacement would be `LocalDate.now().getYear() - 1900`, which nobody would want.) The same applies to `Date.getMonth()`, which is zero-based (September is `8`) where `LocalDate.getMonthValue()` gives `9`. Both were confirmed by running the code: `getYear()` returned `126` and `getMonth()` returned `8` for September 2026, against `2026` and `9` from `java.time`.

**Lab Sheet 3, Task 7 — Deprecated API usage.** *Replace deprecated API calls with modern Java alternatives.*

```java
// BEFORE
public class DateTimeBadUtil {
    public int getCurrentYear(){ Date d=new Date(); return d.getYear(); }
    public int getCurrentMonth(){ Date d=new Date(); return d.getMonth(); }
}
```

```java
// AFTER
public final class DateTimeUtil {
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Colombo");

    private DateTimeUtil() {
        throw new AssertionError("DateTimeUtil is a utility class and must not be instantiated");
    }

    public static int currentYear() {
        return LocalDate.now(DEFAULT_ZONE).getYear();
    }

    public static int currentMonth() {
        return LocalDate.now(DEFAULT_ZONE).getMonthValue();
    }
}
```

*Implementation Analysis.* Both deprecated calls were replaced by `java.time` calls. The class was also renamed (Task 1) and made a proper utility class (Task 10). The time zone is a named constant (Task 4). As the flag above explains, the printed year and month change from `126` and `8` to `2026` and `9`; that is a correction, not a regression.

**Possible Exam Angles (with model answers)**

**Q1:** The compiler prints "Note: uses or overrides a deprecated API." What does it mean and what should you do?

**A1:** The code calls an element its authors have marked as discouraged, because a better replacement exists and it may be removed later. Recompile with `-Xlint:deprecation` to see exactly which calls are involved, read the documentation for the recommended replacement, and switch to it.

**Q2:** Replace the deprecated call and explain why the number may change: `int y = new Date().getYear();`

**A2:** `int y = LocalDate.now().getYear();`. The old method returned the year minus 1900 (for example `125` for 2025), so the replacement returns a different, correct value (`2025`). Any code that relied on the odd old value must be adjusted.

**Q3:** Why "always prefer supported alternatives" to deprecated APIs?

**A3:** Deprecated APIs may be removed in a later release (breaking the program on upgrade), often have known design problems (such as the zero-based month and the offset year), and are no longer improved or fixed.

---

### 4.6 Type-Safe Collections with Generics

> ⚠️ **Flag (lab-only section):** This section is written from Lab Sheet 3 Task 6 and its linked generics article. It is not on the slides. Every code claim in it (the `ClassCastException`, the compiler warnings, the compile-time error on `names.add(42)`) was checked by compiling and running.

**Prerequisites.** A *collection* is an object that holds a group of other objects (for example a `List` or a `Map`). A *raw type* is a generic class used without saying what it holds, such as `List` on its own. *Generics* let you state the element type in angle brackets, as in `List<String>` (a list of `String` values) or `Map<String, String>` (keys and values are both `String`). A *parameterized type* is a generic type with its type arguments filled in. The *diamond operator* `<>` on the right-hand side of an assignment (`new ArrayList<>()`) lets the compiler infer the type arguments from the left-hand side.

**Why raw types are bad.** A raw collection accepts any object, so the compiler cannot check what goes in or comes out, and every read needs a manual cast. A wrong type is only discovered when the program runs, as a `ClassCastException`. With a parameterized type the compiler rejects the mistake before the program runs. Both were confirmed by running: a raw `List` accepted a `String` and an `Integer` together and then failed with `ClassCastException` on the cast, while compiling with all warnings on reported `rawtypes` and `unchecked` warnings for the raw code and none for `List<String>`.

```java
// Raw type: compiles (with warnings), fails later
List names = new ArrayList();
names.add("Alice");
names.add(42);                  // accepted without complaint
String first = (String) names.get(1);   // ClassCastException at run time
```

```java
// Parameterized type: the mistake is a compile-time error
List<String> names = new ArrayList<>();
names.add("Alice");
// names.add(42);               // does not compile
String first = names.get(0);    // no cast needed
```

**Lab Sheet 3, Task 6 — Collection type safety.** *Replace raw collections with parameterized generic types.*

| Where | Before | After |
|---|---|---|
| `PropertiesLoader.loadAll` | `public Map loadAll()`; `Map m = new HashMap();` | `public static Map<String, String> loadAll()`; `Map<String, String> values = new HashMap<>();` |
| `Repository.loadEmployees` | `public List loadEmployees()` returning `Arrays.asList("Alice","Bob")` (plain strings) | `List<Employee> loadEmployees()` returning `List.of(new Employee("Alice"), new Employee("Bob"))` |
| `EmployeeService.processEverything` | `processEverything(List employees)` with `(Employee) o` casts | `processEmployees(List<Employee> employees)`; no cast |
| `Main` | `java.util.List employees = repo.loadEmployees();` under `@SuppressWarnings({"rawtypes","unchecked"})` | `List<Employee> employees` obtained from the service; no suppression |
| `EmployeeOperations` | methods took `Object e` | methods take `Employee employee` |

*Implementation Analysis.* The same change fixes three problems: casts disappear, the compiler now checks every use, and the `@SuppressWarnings` that hid the warnings could be deleted. The repository's loader also stopped returning plain strings for employees, which had forced later code to cast and to treat a name as an employee. The refactored project compiles with every compiler warning enabled and treated as an error (`-Xlint:all -Werror`) with no warnings, where the original produced raw-type, unchecked and deprecation warnings.

**Possible Exam Angles (with model answers)**

**Q1:** Fix the code and explain what improves: `Map cache = new HashMap(); cache.put("k", 1); int v = (Integer) cache.get("k");`

**A1:** `Map<String, Integer> cache = new HashMap<>(); cache.put("k", 1); int v = cache.get("k");`. The compiler now checks that keys are `String` and values `Integer`, the cast disappears, and a wrong type (for example `cache.put(1, "k")`) becomes a compile-time error instead of a run-time `ClassCastException`.

**Q2:** What is the diamond operator?

**A2:** The `<>` in `new ArrayList<>()`: it tells the compiler to infer the type arguments from the declared type on the left, so they need not be written twice.

**Q3:** Why does `List<Object>` not solve the raw-type problem?

**A3:** It is a parameterized type but a useless one: it accepts anything and every read is an `Object` that needs a cast, so the compiler still cannot catch a wrong type. Use the most specific element type that describes the data (`List<Employee>`).

---

### 4.7 Organizing Code Effectively

**The practice.** Four guidelines:

1. **Create utility or helper classes for reusable logic**, so the same code is not repeated in several classes.
2. **Follow the Single Responsibility Principle:** one class, one responsibility (a class should have one reason to change).
3. **Group related functionality logically in packages or modules,** aiming for **high cohesion and low coupling.** *Cohesion* is how strongly the parts of one class or package belong together (high is good); *coupling* is how strongly one class depends on the details of another (low is good).
4. **Avoid bloated utilities.** A utility class that keeps collecting unrelated methods becomes a God Class (a class with too many responsibilities, one of the code smells). Keep utility classes focused.

**Helper versus utility class.** The two kinds of class differ as follows (the difference is set out in a linked article on Java helper and utility classes, listed under further reading):

| | Helper class | Utility class |
|---|---|---|
| Purpose | Provides supporting functionality for the running of a program: repetitive tasks that are not the application's core purpose | Provides static methods for common routines used across an application |
| Instances | Can be instantiated; several instances can exist | Cannot be instantiated |
| State | May have instance variables | Usually stateless |
| Methods | Instance and static | All methods are `static` |
| How to declare | Ordinary class | `final` class with a `private` constructor (which may be empty) so Java does not create a default public one |
| How used | Through an object, and its static methods through the class name | Through the class name, for example `Math.max(...)` |

**Lab Sheet 3, Task 9 — Cohesion and separation of concerns.** *Identify classes or methods doing multiple unrelated tasks and refactor for better cohesion.* Three classes mixed unrelated jobs:

| Class (original) | Mixed responsibilities | Refactored |
|---|---|---|
| `Utility` | Name formatting **and** printing an object to the console (`dumpToConsole`) | Formatting moved to `NameFormatter`; the console-dump method was unused and deleted |
| `Department` | Identity data, translating a numeric code to a level name (twice), **and** building a sentence about an employee | Level names moved to the `DepartmentLevel` enum; the employee sentence moved to `Employee.describeWorkplace` |
| `EmployeeService` | Storage, payslips, CSV export, promotion e-mail, org chart, name formatting, looping and printing | Five small role interfaces; formatting in `NameFormatter`; the service implements only the three it supports |

*Implementation Analysis.* These changes are the Single Responsibility Principle applied at class level; they are described in more detail in Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells). The test for the result is the reason-to-change test: each class now changes for one kind of reason only.

**Lab Sheet 3, Task 10 — Utility class refactoring.** *Refactor utility classes to use static methods and prevent instantiation.* The provided utility classes had non-static methods, so a caller had to write `new Utility().formatName(...)` to use a function that needs no object:

```java
// BEFORE (excerpt)
public class Utility {
    public  String formatName(String name) { /* ... */ }      // instance method: needs an object
}
```

```java
// AFTER
public final class NameFormatter {
    private NameFormatter() {
        throw new AssertionError("NameFormatter is a utility class and must not be instantiated");
    }

    public static String format(String name) { /* ... */ }    // static: NameFormatter.format(...)
}
```

*Implementation Analysis.* Three things make it a proper utility class: the methods are `static`; the class is `final` (it cannot be extended); and the constructor is `private`, so no other class can create an instance. Throwing `AssertionError` from the constructor additionally stops instantiation through reflection (the technique of calling private members by name), and this was tested. The same pattern was applied to `LoggerFactory`, `DateTimeUtil` and `PropertiesLoader`. `HashUtil` (no hashing in it, only a name formatter) was deleted, because its only method duplicated `NameFormatter.format`.

**Possible Exam Angles (with model answers)**

**Q1:** A class `Helper` has `parseDate`, `sendMail`, `hashPassword`, `formatCurrency` and `exportPdf` as static methods. What smell is this and what should be done?

**A1:** A bloated utility class, which is a God Class in disguise: unrelated responsibilities collected in one place. Split it into focused classes such as `DateParser`, `MailSender`, `PasswordHasher`, `CurrencyFormatter` and `PdfExporter`, and keep each utility class small and cohesive.

**Q2:** List the three features of a well-formed utility class in Java.

**A2:** All methods are `static`; the class is declared `final`; and it has a `private` constructor (optionally throwing an error) so it cannot be instantiated or extended.

**Q3:** Explain "high cohesion, low coupling" and why both are wanted.

**A3:** High cohesion means everything in a class or package serves one purpose, so it is easy to understand and changes for one reason only. Low coupling means classes depend on each other only through small, stable contracts, so a change in one does not force changes in the others. Together they make code easier to maintain, test and reuse.

---

### 4.8 Why Best Practices and Coding Standards Are Not Enough

Coding standards and best practices keep code readable and correct on a day-to-day basis, but they do not keep a system healthy over the years. Five reasons:

1. **Requirements evolve** (new features, larger scale, new compliance rules), so the original design stops being adequate, however cleanly it was written.
2. **Understanding of the problem domain improves.** As the team learns the business, the models, names and boundaries in the code must be reshaped to match the domain.
3. **Software entropy.** *Entropy* here means the tendency of a system to become more disordered over time. Many small changes, each clean on its own, still accumulate coupling and duplication over time.
4. **Style is not architecture.** Conventions (naming, formatting) ensure the code is readable, but they do not make the *structure* good: a class can follow every naming rule and still have ten responsibilities.
5. **Delivery trade-offs create technical debt.** Deadline compromises leave code smells behind, and the debt grows until it is repaid.

The answer to these problems is **refactoring**, which addresses them safely (Section 4.9, Refactoring: From Code Smells to Fundamentals and Process).

**Possible Exam Angles (with model answers)**

**Q1:** A project follows the Google Java Style Guide perfectly and all names are correct, yet adding a feature takes weeks because every change touches many classes. Why did standards not prevent this, and what would help?

**A1:** Style conventions guarantee readable code, not good architecture, and small clean changes still accumulate coupling and duplication over time (software entropy). Refactoring the structure (for example, moving scattered logic into one class) restores the design.

**Q2:** State two reasons a design that was good at the start can become inadequate later.

**A2:** Requirements evolve (features, scale, compliance), and the team's understanding of the problem domain improves, so the original models, names and boundaries no longer match reality.

**Q3:** Is "our code follows the coding standard" evidence that the design is healthy? Explain.

**A3:** No. A coding standard is about uniformity and readability (style), not about cohesion, coupling or responsibility placement (architecture). Code can satisfy every convention and still contain smells such as God Classes or duplicated logic.

---

### 4.9 Refactoring: From Code Smells to Fundamentals and Process

**Code smells and refactoring.** A *code smell* is a symptom in code that may indicate a deeper problem: a warning, not a bug. Smells make code harder to maintain and extend. Refactoring removes smells and restores design integrity. Three examples of a smell paired with its usual refactoring:

| Smell | What it is | Refactoring |
|---|---|---|
| Long Method | A method that tries to do too much | Extract Method |
| Large Class | A class with too many responsibilities | Extract Class |
| Switch/If-Else Chains | A long conditional that chooses behaviour by type | Replace Conditional with Polymorphism (often shortened to "Replace with Polymorphism") |

The complete mapping of smells to techniques is in Section 4.11 (The Refactoring Techniques Catalogue and Choosing a Technique).

**Definition.** Refactoring is "a change made to the internal structure of software to make it easier to understand and cheaper to modify without changing its observable behavior" (Martin Fowler). Fowler's catalogue also gives the verb form: to restructure software by applying a series of such changes without changing its observable behaviour. The key phrase is *without changing observable behavior*: anything a user or another program can see (results, outputs, interfaces) stays the same. Refactoring is therefore not adding features and not fixing bugs, although it can help find bugs.

**Goals:**

- Improve the design.
- Make the code easier to understand.
- Help find bugs (clearer code makes mistakes easier to see).
- Enable faster development.

**Small Steps Principle.** Make incremental changes, one at a time, running the tests after each. Small steps reduce risk, because when something breaks the last change is the only suspect.

**Why the effort pays back.** Design quality affects how fast a team can keep adding functionality. In the comparison of good and poor design over time, the poor design gives a slightly faster start, but its cumulative functionality soon flattens because every change becomes harder; with good design the cumulative functionality keeps rising steadily, and the two curves cross early. The comparison is about how easy code is to modify, and comes from Fowler's book on refactoring (2nd edition). Refactoring is how a poor design is turned into the second kind.

**The refactoring process.**

1. Identify a problem (a code smell).
2. Write or verify tests *before* changing anything.
3. Apply one or more refactoring techniques.
4. Re-run the tests to confirm the behaviour has not changed.
5. Repeat: refactoring is continuous, not a one-off.

Step 2 is the important safeguard: tests are the only practical way to show that behaviour was preserved (which is why, as Section 4.12 (When Not to Refactor) explains, weak test coverage is a reason to hold back).

> ⚠️ **Flag (extra guidance from refactoring.guru, not on the slides):** The slides send you to refactoring.guru for the techniques. That site also states three rules of how to refactor (the code should become cleaner; no new functionality should be created during refactoring; all existing tests must pass afterwards) and gives the *Rule of Three* for when to refactor: the first time you do something, just do it; the second time, tolerate the repetition; the third time, refactor. It also suggests refactoring when adding a feature, when fixing a bug and during code review. These were read from a reproduction of the site's text and match the process above.

**Possible Exam Angles (with model answers)**

**Q1:** Which of these is a refactoring? (a) renaming a method to explain what it does; (b) adding a discount feature; (c) fixing a wrong tax calculation; (d) splitting a 200-line method into smaller methods.

**A1:** (a) and (d). Refactoring changes internal structure without changing observable behaviour. Adding a feature (b) changes what the program does, and fixing a wrong result (c) also changes observable behaviour.

**Q2:** Put the refactoring steps in order: apply the technique; identify the smell; re-run the tests; write or verify tests; repeat.

**A2:** Identify the smell; write or verify tests; apply the technique; re-run the tests; repeat.

**Q3:** Explain the Small Steps Principle and why it matters.

**A3:** Refactor through small incremental changes, running the tests after each. If a test fails, only the last small change can be responsible, so problems are found and fixed immediately and the risk of breaking behaviour stays low.

**Q4:** Fowler says refactoring must not change "observable behavior". Give one example of a change that would break this rule.

**A4:** Changing what a public method returns for the same input (for example altering a total), removing a method other code calls, or changing printed output that another system parses. Any of these changes something users or other programs can observe.

---

### 4.10 Refactoring Examples: Extract Method, Introduce Parameter Object, Replace Magic Number with Symbolic Constant

Three techniques are demonstrated, each as a problem and a solution.

**Example 1 — Extract Method**

*Problem:* you have a code fragment that can be grouped together.

```java
public void printInvoice(Order order) {
    System.out.println("Invoice for " + order.getCustomerName());
    double total = 0;
    for (Item item : order.getItems()) {
        System.out.println(item.getName() + ": " + item.getPrice());
        total += item.getPrice();
    }
    System.out.println("Total: " + total);
}
```

*Solution:* move this code to a separate new method and call it from the old code.

```java
public void printInvoice(Order order) {
    printHeader(order);
    printDetails(order);
}

private void printHeader(Order order) {
    System.out.println("Invoice for " + order.getCustomerName());
}

private void printDetails(Order order) {
    double total = 0;
    for (Item item : order.getItems()) {
        System.out.println(item.getName() + ": " + item.getPrice());
        total += item.getPrice();
    }
    System.out.println("Total: " + total);
}
```

*Implementation Analysis.* The original method does two different things (print a heading, print the lines and total). The new top-level `printInvoice` reads like an outline of the job, and each new method has a name that states its purpose. The new methods are `private` because only `printInvoice` needs them. The output is exactly the same, which is the definition of a refactoring. `printDetails` still both prints lines and computes the total, so a further Extract Method (for example a `calculateTotal`) would be possible; refactoring can be repeated until each method does one thing.

*In the lab project:* the long `processEverything` method was reduced to `processEmployees` and a one-line `logDisplayName`, with the name formatting extracted into `NameFormatter.format` (Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells) shows this under the Long Method smell).

**Example 2 — Introduce Parameter Object**

*Problem:* your methods contain a repeating group of parameters.

```java
public void createOrder(String customerName, String street, String city
    , String postalCode, List<Item> items) {
    // Implementation
}

public void estimateDelivery (String customerName, String street,
    String city, String postalCode, List<Item> items) {
    // Implementation
}
```

*Solution:* replace these parameters with an object.

```java
public void createOrder(Customer customer, List<Item> items) {
    // Implementation
}

class Customer {
    String name;
    Address address;
}

class Address {
    String street;
    String city;
    String postalCode;
}
```

*Implementation Analysis.* `customerName`, `street`, `city` and `postalCode` travel together through both methods; that repeated group is a Data Clump (a group of variables that appear together) and produces a Long Parameter List. Replacing the four parameters with one `Customer` object shortens both signatures, removes the risk of passing the strings in the wrong order (all four are `String`, so the compiler cannot tell), and gives the address logic a home. Only `createOrder` is shown converted; `estimateDelivery` would be changed the same way.

> ⚠️ **Flag (refactoring.guru's cautions for this technique):** refactoring.guru advises making the new parameter class *immutable* (its fields cannot change after creation) and warns that if you move only data into the new class and no behaviour, it begins to smell of a Data Class (a class that only holds fields). The example's `Customer` and `Address` have plain modifiable fields and no behaviour, so they show the shape of the technique only. Its recommended steps are: create the class; add it as a new parameter; delete the old parameters one at a time, testing after each; then see whether any behaviour belongs in the new class (Move Method or Extract Method).

*In the lab project:* the loose `city` and `zip` fields of `Employee` became an immutable `Address` record (`public record Address(String city, String zip) { }`).

**Example 3 — Replace Magic Number with Symbolic Constant**

*Problem:* your code uses a number that has a certain meaning to it.

```java
double circumference = 2 * 3.14159 * radius;
```

*Solution:* replace this number with a constant that has a human-readable name explaining the meaning of the number.

```java
public static final double PI = 3.14159;
double circumference = 2 * PI * radius;
```

*Implementation Analysis.* A magic number is a numeric value in the source that has no obvious meaning. Naming it turns it into live documentation and makes it easy to change in one place. A plain find-and-replace is not a safe alternative: the same number may be used for different purposes in different places, so each use has to be checked. (In real Java code, `Math.PI` already exists and should be used instead of defining your own.) The short title "Replace Magic Numbers with Constants" and the standard catalogue name *Replace Magic Number with Symbolic Constant* refer to the same technique.

*In the lab project:* `MINIMUM_SALARY`, `EMPLOYEE_ID_FORMAT`, `REPORT_DONE_MESSAGE` and the `DepartmentLevel` enum (Section 4.4, Constants and External Configuration).

**Possible Exam Angles (with model answers)**

**Q1:** Name the smell and the technique, and rewrite:
```java
void report(Order o) {
    // print header
    System.out.println("== " + o.getId() + " ==");
    // compute total
    double t = 0;
    for (Item i : o.getItems()) t += i.getPrice();
    // print footer
    System.out.println("Total: " + t);
}
```

**A1:** Long Method; the comments mark natural boundaries, so use Extract Method: `void report(Order o) { printHeader(o); double t = calculateTotal(o); printFooter(t); }` with `printHeader`, `calculateTotal` and `printFooter` as small methods named after the comments.

**Q2:** Name the smell(s) and technique: `void book(String guest, String passport, String country, LocalDate from, LocalDate to)` and `void cancel(String guest, String passport, String country)`.

**A2:** Long Parameter List and Data Clumps (`guest`, `passport`, `country` always travel together). Technique: Introduce Parameter Object: a `Guest` object holding those three, and possibly a `DateRange` for `from` and `to`.

**Q3:** Which technique fixes `if (speed > 120) fine();`? Write the result.

**A3:** Replace Magic Number with Symbolic Constant: `private static final int SPEED_LIMIT = 120;` and `if (speed > SPEED_LIMIT) fine();`.

**Q4:** After an Extract Method, a colleague says "the output changed slightly, but it is much cleaner, so it counts as refactoring." Is this correct?

**A4:** No. Refactoring must not change observable behaviour. If the output changed, the change is not a pure refactoring; either the extraction was done wrongly or it also changed behaviour and should be treated (and tested) as a functional change.

---

### 4.11 The Refactoring Techniques Catalogue and Choosing a Technique

Quiz questions ask for "the best refactoring technique" for a code snippet, so this section lists the standard techniques by group, maps every code smell to its usual techniques, and separates the look-alikes.

> ⚠️ **Flag (catalogue is a supplement; how far it was verified):** The slides send you to the *Refactoring Techniques* catalogue on refactoring.guru without reproducing it, so this section is a supplement built from that catalogue's wording. What was checked, and where: (1) read on refactoring.guru's own pages: *Introduce Parameter Object*, *Remove Middle Man*, *Extract Superclass*, the whole *Simplifying Method Calls* group, and the smell pages for Switch Statements, Divergent Change, Feature Envy, Dead Code, Alternative Classes with Different Interfaces, Inappropriate Intimacy, Shotgun Surgery, Message Chains, Middle Man, Long Method and Temporary Field, plus Duplicate Code through mirrors of the site's text; (2) read in copies of the site's text (sourcemaking.com and a public repository): *Extract Method*, *Replace Magic Number with Symbolic Constant*, the *Composing Methods*, *Moving Features between Objects* and *Organizing Data* groups, and the treatments for Large Class, Primitive Obsession, Data Clumps and Long Parameter List. Only the technique *names* are given for the "Simplifying Conditional Expressions" and "Dealing with Generalization" groups except where a description is shown; the descriptions of Replace Conditional with Polymorphism and Decompose Conditional come from a university summary of Fowler's book.

> ⚠️ **Flag (shorthand names on the slides):** The slides shorten some names: "Replace with Polymorphism" for *Replace Conditional with Polymorphism*, and "Replace Magic Numbers with Constants" for *Replace Magic Number with Symbolic Constant*. In a multiple-choice quiz, treat the short and full forms as the same technique.

**The six groups:**

| Group | What its techniques do |
|---|---|
| **Composing Methods** | Streamline methods, remove duplication and prepare for later improvements |
| **Moving Features between Objects** | Safely move functionality between classes, create new classes and hide implementation details |
| **Organizing Data** | Improve data handling, replace primitives with rich classes and untangle class associations |
| **Simplifying Conditional Expressions** | Combat conditionals that grow more complicated over time |
| **Simplifying Method Calls** | Make method calls simpler and easier to understand, which simplifies the interfaces between classes |
| **Dealing with Generalization** | Move functionality along the inheritance hierarchy, create classes and interfaces, and swap inheritance for delegation and back |

**Composing Methods** (problem → solution):

- **Extract Method:** a code fragment can be grouped together → move it to a new method and replace the old code with a call.
- **Inline Method:** a method body is more obvious than the method itself → replace calls with the method's content and delete the method.
- **Extract Variable:** an expression is hard to understand → place the expression or its parts in self-explanatory variables.
- **Inline Temp:** a temporary variable holds the result of a simple expression and nothing more → replace references to it with the expression.
- **Replace Temp with Query:** the result of an expression is stored in a local variable for later use → move the expression to a separate method and call that method instead of using the variable.
- **Split Temporary Variable:** one local variable stores several different intermediate values → use a different variable for each value.
- **Remove Assignments to Parameters:** a value is assigned to a parameter inside the method → use a local variable instead.
- **Replace Method with Method Object:** a long method has local variables so intertwined that Extract Method cannot be applied → turn the method into its own class, so the local variables become fields, then split it into several methods.
- **Substitute Algorithm:** you want to replace an existing algorithm with a new one → replace the body of the method with the new algorithm.

**Moving Features between Objects:**

- **Move Method:** a method is used more in another class than in its own → create a new method in the class that uses it most, move the code there, and turn the old method into a reference to it or remove it.
- **Move Field:** a field is used more in another class than in its own → create the field in that class and redirect all users of the old field.
- **Extract Class:** one class does the work of two → create a new class and place the fields and methods responsible for the extra functionality in it.
- **Inline Class:** a class does almost nothing and no further responsibilities are planned for it → move all its features to another class.
- **Hide Delegate:** the client gets object B from object A and then calls a method of B → create a method in A that delegates the call to B, so the client no longer knows about B.
- **Remove Middle Man:** a class has too many methods that only delegate to other objects → delete those methods and make the client call the end methods directly.
- **Introduce Foreign Method:** a utility class lacks a method you need and you cannot add it → add the method to a client class and pass an object of the utility class to it as an argument.
- **Introduce Local Extension:** a utility class lacks several methods you need and you cannot add them (its solution is a new class that extends or wraps the utility class).

**Organizing Data:**

- **Self Encapsulate Field:** a class accesses its own private fields directly → create a getter and setter and use only them.
- **Replace Data Value with Object:** a data field has its own behaviour and associated data → create a new class for it and hold an object of that class.
- **Change Value to Reference:** many identical instances of one class → convert them to a single reference object.
- **Change Reference to Value:** a reference object is too small and too rarely changed to justify managing its life cycle → turn it into a value object.
- **Replace Array with Object:** an array holds different types of data → replace it with an object that has a separate field for each element.
- **Duplicate Observed Data:** domain data is stored in classes responsible for the user interface → separate it into domain classes and keep the two in step.
- **Change Unidirectional Association to Bidirectional** and **Change Bidirectional Association to Unidirectional:** add a missing association that a class needs; or remove one that a class does not use.
- **Replace Magic Number with Symbolic Constant:** a number in the code has a meaning → replace it with a named constant.
- **Encapsulate Field:** a public field → make it private and provide access methods.
- Also in this group (names only): **Encapsulate Collection**, **Replace Type Code with Class**, **Replace Type Code with Subclasses**, **Replace Type Code with State/Strategy**, **Replace Subclass with Fields**.

**Simplifying Conditional Expressions** (names, with the two described ones):

- **Decompose Conditional:** a complicated conditional → extract the condition, the "then" part and the "else" part into separate, well-named methods.
- **Replace Conditional with Polymorphism:** a conditional chooses different behaviour depending on an object's type → move each branch into an overriding method in a subclass and make the original method abstract.
- Also (names only): **Consolidate Conditional Expression**, **Consolidate Duplicate Conditional Fragments**, **Remove Control Flag**, **Replace Nested Conditional with Guard Clauses**, **Introduce Null Object**, **Introduce Assertion**.

**Simplifying Method Calls:**

- **Rename Method:** the name does not explain what the method does → rename it.
- **Add Parameter:** a method does not have enough data to do its job → add a parameter for the data.
- **Remove Parameter:** a parameter is not used in the body → remove it.
- **Separate Query from Modifier:** a method returns a value and also changes something in an object → split it into two methods, one to return the value and one to modify the object.
- **Parameterize Method:** several methods do similar things that differ only in internal values, numbers or operations → combine them into one method with a parameter for the special value.
- **Replace Parameter with Explicit Methods:** a method is split into parts, each run depending on the value of a parameter → extract each part into its own method and call those instead.
- **Preserve Whole Object:** you get several values from an object and pass them as parameters → pass the whole object instead.
- **Replace Parameter with Method Call:** a query method's result is passed as a parameter to another method that could call the query itself → put the query call inside the method body.
- **Introduce Parameter Object:** methods contain a repeating group of parameters → replace the parameters with an object.
- **Remove Setting Method:** a field's value should be set only when the object is created and never change → remove the setter methods.
- **Hide Method:** a method is not used by other classes, or only inside its own class hierarchy → make it private or protected.
- Also (names only): **Replace Constructor with Factory Method**, **Replace Error Code with Exception**, **Replace Exception with Test**.

**Dealing with Generalization:**

- **Extract Superclass:** two classes have common fields and methods → create a shared superclass and move the identical fields and methods to it.
- Also (names only): **Pull Up Field**, **Pull Up Method**, **Pull Up Constructor Body**, **Push Down Method**, **Push Down Field**, **Extract Subclass**, **Extract Interface**, **Collapse Hierarchy**, **Form Template Method**, **Replace Inheritance with Delegation**, **Replace Delegation with Inheritance**.

**Every smell mapped to its usual techniques** (refactoring.guru's treatments; fixes shown first are the most common):

| Smell | Technique(s) |
|---|---|
| Long Method | Extract Method; if local variables get in the way: Replace Temp with Query, Introduce Parameter Object, Preserve Whole Object, or Replace Method with Method Object; for tangled conditionals: Decompose Conditional |
| Large Class (God Class) | Extract Class; Extract Subclass; Extract Interface |
| Primitive Obsession | Replace Data Value with Object; for parameters: Introduce Parameter Object or Preserve Whole Object; for type codes: Replace Type Code with Class, with Subclasses, or with State/Strategy; for arrays: Replace Array with Object |
| Data Clumps | Extract Class (if the clump is fields); Introduce Parameter Object (if it is in parameters); Preserve Whole Object |
| Long Parameter List | Replace Parameter with Method Call; Preserve Whole Object; Introduce Parameter Object |
| Switch/If-Else Chains | Extract Method then Move Method to isolate the switch; Replace Type Code with Subclasses or State/Strategy; then Replace Conditional with Polymorphism; Replace Parameter with Explicit Methods (when there are few conditions that all call the same method with different parameters); Introduce Null Object (when one option is `null`). Leave it alone when the switch only performs simple actions or a factory uses it to choose which class to create |
| Temporary Field | Extract Class (or a subclass, as in Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells)); Introduce Null Object |
| Alternative Classes with Different Interfaces | Rename Methods; Move Method, Add Parameter, Parameterize Method; Extract Superclass; then possibly delete one class |
| Divergent Change | Extract Class (and Extract Superclass or Subclass if the parts share behaviour) |
| Shotgun Surgery | Move Method and Move Field into one class; then Inline Class for classes left almost empty |
| Duplicate Code | Extract Method (same class); Extract Method then Pull Up Field (sibling subclasses); Form Template Method (similar code); Substitute Algorithm (different algorithms, same result); Extract Superclass or Extract Class (unrelated classes) |
| Dead Code | Delete the unused code and unneeded files (a good IDE is the quickest way to find it); Inline Class or Collapse Hierarchy for an unneeded class; Remove Parameter for unused parameters. Dead code can also hide as an unreachable branch of a complex conditional |
| Feature Envy | Move Method; Extract Method first if only part of the method is envious; if it uses several classes, put it in the class holding most of the data it uses. Leave it alone when behaviour is deliberately kept separate from the data class (for example Strategy or Visitor) |
| Inappropriate Intimacy | Move Method and Move Field; Extract Class; Hide Delegate; Change Bidirectional Association to Unidirectional |
| Message Chains | Hide Delegate; or Extract Method then Move Method to the start of the chain |
| Middle Man | Remove Middle Man |

**Look-alike techniques and how to choose:**

| Confusable pair or group | How to choose |
|---|---|
| Extract Method vs Replace Method with Method Object | Extract Method is the default. Use the method object only when the method's local variables are so intertwined that Extract Method cannot be applied. |
| Extract Class vs Extract Superclass vs Extract Subclass | Extract Class splits *responsibilities* of one class into a separate class that it uses. Extract Superclass is for two classes that share fields and methods. Extract Subclass is for features used only in some cases. |
| Move Method vs Extract Method | Move Method relocates a whole method to the class whose data it uses. If only part of a method is envious, Extract Method on that part first, then move it. |
| Hide Delegate vs Remove Middle Man | Opposites. Hide Delegate removes a client's need to navigate a chain; Remove Middle Man removes a class that has become pure forwarding. Hiding too much creates a Middle Man. |
| Introduce Parameter Object vs Preserve Whole Object vs Replace Parameter with Method Call | Introduce Parameter Object: a repeating group of unrelated values. Preserve Whole Object: the values all come from one object, so pass the object. Replace Parameter with Method Call: the callee can get the value itself. |
| Replace Data Value with Object vs Replace Type Code with Class | Replace Data Value with Object: a data field has its own behaviour and data. Replace Type Code with Class: an integer or string code stands for a fixed set of kinds. |
| Replace Conditional with Polymorphism vs Decompose Conditional | Polymorphism when the branches are chosen by *type* and differ in behaviour; Decompose Conditional when the problem is that the condition itself is hard to read. |
| Extract Method vs Inline Method | Opposites: Inline Method removes a method whose body is clearer than its name. |
| Rename Method vs Extract Method | Rename when the code is fine but the name does not explain it; Extract when a fragment needs a name of its own. |

**Possible Exam Angles (with model answers): choosing the best technique**

**Q1:** Name the smell and the best technique.
```java
double bonus(Employee e) {
    return e.getYears() * e.getRating() * e.getBaseSalary();
}   // this method is in class Payroll
```

**A1:** Feature Envy: the method uses only `Employee` data. Best technique: Move Method (put `calculateBonus()` in `Employee`).

**Q2:** Name the smell and the best technique: `String city = order.getCustomer().getAddress().getCity();` appears in twelve places.

**A2:** Message Chains. Best technique: Hide Delegate (add `order.getCustomerCity()`). If a class then fills up with forwarding methods, that is the start of a Middle Man.

**Q3:** Name the smell and best technique: a class whose every method just calls the same method on another object.

**A3:** Middle Man. Best technique: Remove Middle Man (callers use the real class directly).

**Q4:** Name the smell and best technique: `if (type == CAR) {...} else if (type == BIKE) {...} else if (type == TRUCK) {...}` repeated in four methods.

**A4:** Switch/If-Else Chains (repeated). Best technique: Replace Conditional with Polymorphism: a `Vehicle` type with `Car`, `Bike` and `Truck` subclasses (or a strategy), each supplying its own behaviour. If the chain switches on a numeric or string code, first apply Replace Type Code with Subclasses (or with State/Strategy).

**Q5:** Name the smell and best technique: five `Customer` classes in different packages each contain identical `validateEmail` methods.

**A5:** Duplicate Code across unrelated classes. Best technique: Extract Class (a shared `EmailValidator`) used by all five, or Extract Superclass if the classes are closely related.

**Q6:** Name the smell and best technique: a 150-line method with many local variables that depend on each other so tightly that extracting pieces would need a dozen parameters.

**A6:** Long Method where Extract Method is blocked by intertwined local variables. Best technique: Replace Method with Method Object (turn the method into a class so the locals become fields, then split it into methods).

**Q7:** Name the smell and best technique: a `Report` class with a `formatFor(String type)` method whose body is `if (type.equals("PDF")) {...} else if (type.equals("HTML")) {...}`.

**A7:** Switch/If-Else Chains on a parameter. Best technique: Replace Parameter with Explicit Methods (`formatAsPdf`, `formatAsHtml`) or Replace Conditional with Polymorphism (a `Formatter` interface with one class per format), the second being better if new formats will keep being added.

**Q8:** Name the smell and best technique: class `Order` has a method `total()` and class `Invoice` has a method `getSum()`; both add up the item prices in exactly the same way, but because the names differ they cannot be used interchangeably.

**A8:** Alternative Classes with Different Interfaces (or Duplicate Code, if the bodies are identical). Best technique: Rename Method so they match; then, if they share behaviour, Extract Superclass.

**Q9:** A method has the parameters `(int day, int month, int year, int hour, int minute)` and three other methods take the same five.

**A9:** Long Parameter List and Data Clumps. Best technique: Introduce Parameter Object: one `DateTimeValue` object holding the five values.

**Q10:** You write `String a = customer.getStreet(); String b = customer.getCity(); shipper.ship(a, b);`, i.e. several values fetched from one object and passed on. Which technique?

**A10:** Preserve Whole Object: call `shipper.ship(customer)` and let `ship` read what it needs.

---

### 4.12 When Not to Refactor

Refactoring costs effort and carries risk, so it is not always the right choice. Five cases:

1. **Performance-sensitive code.** Refactoring that makes code cleaner but adds overhead in critical *hot paths* (code executed extremely often, where small costs add up) may harm performance.
2. **Stable, rarely changed modules.** If a component is stable, well tested and rarely modified, refactoring may not yield enough value to justify the effort.
3. **Impending deadlines.** It is risky to refactor when delivery schedules are tight and there is no time for adequate regression testing (re-running tests to check that existing behaviour still works).
4. **Lack of test coverage.** Without strong tests, refactoring that preserves behaviour cannot be safely guaranteed (this follows from step 2 of the process in Section 4.9, Refactoring: From Code Smells to Fundamentals and Process).
5. **Purely stylistic differences.** Avoid refactoring only for stylistic differences that do not improve maintainability.

**Possible Exam Angles (with model answers)**

**Q1:** A module has no automated tests and its authors have left. A new developer wants to "clean it up" before adding a feature. Should they?

**A1:** Not straight away. Without tests there is no reliable way to prove that behaviour is preserved, so the refactoring is risky. First write tests that capture the current behaviour, then refactor in small steps.

**Q2:** The release is in two days and a class contains a Long Method. Is refactoring now a good idea? Why or why not?

**A2:** Usually not. With impending deadlines there is not enough time for regression testing, so the risk of introducing a defect outweighs the benefit; record it as technical debt and refactor after release.

**Q3:** Give one situation where cleaner code could be the wrong trade.

**A3:** Code in a performance-critical hot path, where a refactoring (for example splitting an inner loop into extra method calls or objects) could add overhead that is noticeable because the code runs extremely often.

**Q4:** Someone proposes renaming every local variable to match a new personal style, with no improvement in clarity. Is this worthwhile?

**A4:** No: refactoring purely for stylistic differences that do not improve maintainability is not worth the effort and adds risk.

---

**Chapter Summary**

Coding standards make code uniform, which reduces complexity in collaboration. Best practices (logging, exception handling, constants and configuration, supported APIs, well-organized code) make code correct and operable day to day. Refactoring keeps the design healthy as software evolves; it improves internal structure without changing observable behaviour, works in small steps protected by tests, and is a habit, not a one-time activity. Good code quality is standards plus best practices plus refactoring.

**Further reading listed for this topic:** the article *Java Helper vs. Utility Classes* (Baeldung), recommended for Section 4.7 (Organizing Code Effectively); Martin Fowler, *Refactoring: Improving the Design of Existing Code* (2nd edition), Chapters 2 and 3; and the *Refactoring Techniques* catalogue on refactoring.guru (the source of the technique names used in Section 4.11, The Refactoring Techniques Catalogue and Choosing a Technique).

**Flags to Review**

Every flag marked in this chapter, in the order it appears. Items 1–5 need only a glance; the rest matter when a detail is tested.

1. **Scope and exclusions** (start of chapter): administrative slides, decorative images and a computer-generated cartoon were left out; the two pictures that carry a point are written out in words.
2. **No past papers, no tutorials** (start of chapter): exam-style questions are calibrated from the quiz brief and the lab sheet only.
3. **Numbering assumptions** (start of chapter): "Lecture 4" came from you, and "Week 4" is assumed; check the chapter number and filename.
4. **Lab sheet split across two chapters** (start of chapter): Part A is answered here; Parts B and C are answered in Week 3's guide (Writing Quality Software – SOLID Principles and Code Smells).
5. **Lab-only content** (start of chapter): Task 6 (generics) and the detail of Task 8 (Javadoc) come from the lab sheet's linked articles, not from the slides.
6. **Age of the naming-conventions page** (Section 4.1, Code Quality, Coding Standards and Naming Conventions): Oracle marks its page "for archive purposes only" and last revised it in 1999; the rules are still the standard ones; the lab sheet's printed link is broken by a line wrap.
7. **ELK is only named** (Section 4.2, Logging): the expansion (Elasticsearch, Logstash, Kibana) was not checked against the page the slide links to.
8. **The logging example uses a different library from the lab** (Section 4.2, Logging): `logger.warn(...)` with `{}` is SLF4J or Log4j style; `java.util.logging` has `warning(...)` and uses `{0}`, checked by compiling and running.
9. **The good exception example's logging call is SLF4J-style** (Section 4.3, Exception Handling): the `java.util.logging` equivalent is given.
10. **Three weaknesses in the printed configuration loader** (Section 4.4, Constants and External Configuration): a Windows-only path separator, the file re-read on every call, and an unused key.
11. **The two printed outputs are different numbers** (Section 4.5, Avoiding Deprecated APIs): `125` versus `2025`; the "replacement" corrects a surprising value rather than preserving it.
12. **Lab-only section** (Section 4.6, Type-Safe Collections with Generics): the section is not on the slides.
13. **Extra guidance from refactoring.guru, not on the slides** (Section 4.9, Refactoring: From Code Smells to Fundamentals and Process): the three rules of how to refactor and the Rule of Three, read from a reproduction of the site's text.
14. **refactoring.guru's cautions for Introduce Parameter Object** (Section 4.10, Refactoring Examples): make the class immutable, and beware a Data Class; the printed example does neither.
15. **Catalogue is a supplement; how far it was verified** (Section 4.11, The Refactoring Techniques Catalogue and Choosing a Technique): which pages were read on refactoring.guru itself and which only in copies of its text is listed.
16. **Shorthand names on the slides** (Section 4.11, The Refactoring Techniques Catalogue and Choosing a Technique): "Replace with Polymorphism" and "Replace Magic Numbers with Constants" are the same as the full catalogue names.
