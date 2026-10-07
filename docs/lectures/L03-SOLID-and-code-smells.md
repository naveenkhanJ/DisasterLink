# SE3070 — Case Studies in Software Engineering — Week 3

## Chapter 3: Writing Quality Software – SOLID Principles and Code Smells

> ⚠️ **Flag (scope and exclusions):** Course-administration slides (title slide, contents list, acknowledgments, closing contact slide) are left out because they carry no teachable content. The decorative pictures (the light-bulb mascot, the closing photograph) are also left out. The slides' *reference list* is kept, because one item in it is marked mandatory (see Section 3.10, Code Smells: What They Are). Ask if you want the administrative slides included as a short reference block.

> ⚠️ **Flag (no past papers):** No past exam paper exists in the project's knowledge base, and this module has no tutorial sheets. The "Possible Exam Angles" questions in this chapter are therefore calibrated from two things only: the quiz brief for the CSSE lab quiz (multiple-choice questions on code snippets: identify code smells, choose the best refactoring technique, identify SOLID violations, and say how to fix them) and the style of the Lab Sheet 3 tasks.

> ⚠️ **Flag (numbering assumptions):** The lecture slides do not state a lecture number. "Lecture 3" comes from you, not from the slides. "Week 3" is assumed to equal the lecture number. Only the chapter number and filename depend on this; correct them if your weeks differ.

> ⚠️ **Flag (lab sheet split across two chapters):** Lab Sheet 3 (its own heading says "Practical Sheet 04") covers two lectures. Part B (SOLID, Tasks 11–15) and Part C (code smells) belong to this chapter. Part A (Tasks 1–10, coding standards and best practices) belongs to Lecture 4 and is answered in Week 4's guide (Best Practices and Refactoring Techniques). The lecture and the lab sheet do line up topically, so no mismatch exists.

**What you should be able to do after this chapter:** explain why a good design can still fail during implementation; apply the five SOLID principles; define a code smell; identify and explain the common smells in object-oriented code; and relate each smell to the design or implementation flaw beneath it.

**How the lab answers are shown.** Lab Sheet 3 asks you to improve a provided Java project called `BadPracticeEmployeeSystem`. The answers below use short before/after excerpts from that project. Every "after" excerpt comes from a refactored version that was compiled with all compiler warnings enabled and run. Where an "after" differs from the original in behaviour, the difference is stated.

---

### 3.1 Design vs. Implementation, and Why Implementations Fail

**Design and implementation are different activities with different concerns.** Software design defines the structure of the software and how its components interact. It covers the user experience, the architecture, which component is responsible for what, and the high-level interactions between components. Implementation turns that design into working code. It covers how the code is structured, which libraries are used, and which frameworks are used. Both are essential, but they are not the same thing, and good design quality does not guarantee good implementation quality. Even a perfect design can fail through poor implementation. For example, a set of well-structured classes can be implemented with duplicated logic, so the design looks clean on paper while the code is hard to change. The aim of this chapter is to bridge the gap between design quality and implementation quality.

**Why implementations fail.** Five causes are named:

1. Developers misinterpret the design.
2. Time pressure and shortcuts lead to careless code and skipped safeguards.
3. There is no code review or quality control.
4. Developers have a poor understanding of design principles.
5. The code is overcomplicated during the coding phase.

**Worked illustration of time pressure: Knight Capital.** A well-known case is Knight Capital, a trading firm that lost about $440 million within a single trading morning. It also illustrates Dead Code (Section 3.11, The Common Code Smells). The essential facts follow.

> ⚠️ **Flag (supplement from outside the slides):** The details below are not on the slides. They were checked against two primary sources: Knight's own filings with the US Securities and Exchange Commission (SEC), which report a technology issue related to installing trading software and a pre-tax loss of about $440 million; and the SEC's 2013 order against Knight Capital Americas LLC (Release No. 34-70694), which describes the mechanism. Two figures differ from the slide: the SEC order puts the erroneous trading at approximately 45 minutes (the slide's video title says 30) and the loss at more than $460 million (Knight's filings say about $440 million). For the quiz, use the slide's figures ($440 million, 30 minutes); the lesson is the same. The slide files this case under "time pressure and shortcuts", but the SEC order documents missing safeguards (no second-person review of the deployment, no written deployment procedures) and does not mention time pressure, so it also fits "lack of code review or quality control".

From 27 July 2012, Knight Capital, a large US equities trading firm, rolled out new code to its automated order router in stages, to take part in a new stock-exchange programme that started on 1 August. The router ran on eight servers. One technician did not copy the new code to one of them, no second person reviewed the deployment, and there was no written procedure requiring a review. The new code reused a flag that had, years earlier, switched on an old function called "Power Peg". Knight had stopped using Power Peg in 2003, but its code had been left in the router, still callable, and had been altered since without being retested. On 1 August the seven updated servers handled orders correctly. On the eighth server, the reused flag woke up the old code, which kept sending orders without regard to how many shares had already been filled. Before the market opened, the firm's systems had already sent 97 automated e-mails saying "Power Peg disabled", but they were not treated as alerts. In about 45 minutes, 212 customer orders led to more than 4 million executions in 154 stocks. Knight reported a pre-tax loss of about $440 million (the SEC order says more than $460 million). The lesson is that a poorly controlled release, combined with unused code left in production (a Dead Code smell), let an old function wake up by accident.

**Historical approaches to reducing implementation failure.** Each of these tried to make code more understandable and less error-prone, and each builds on the previous one:

- **Structured programming:** clear control flow (well-defined loops, conditions and sequences instead of unstructured jumps).
- **Procedural design principles:** code split into modular procedures.
- **Early object-oriented guidelines:** encapsulation, inheritance and polymorphism.
- **SOLID principles:** five more specific principles for object-oriented design, popularised by Robert C. Martin ("Uncle Bob"), covered from Section 3.3 (SOLID Principles: Overview) onward.

**Possible Exam Angles (with model answers)**

**Q1:** A design document shows a clean set of small classes with clear responsibilities. Six months after release the code is very hard to change. Give two implementation-level reasons this can still happen even though the design was good.

**A1:** Design quality and implementation quality are separate. A good design can be undermined by how it is coded: for example, developers may implement the well-structured classes with duplicated logic, so a single change has to be made in many places. Developers may also have misread the design, or rushed under time pressure and skipped code review, so shortcuts accumulated in the code.

**Q2:** Which of the five causes of implementation failure does the Knight Capital incident most directly illustrate, and which code smell was involved?

**A2:** It is an example of time pressure and shortcuts in software delivery, and it equally shows lack of code review or quality control: no second person checked the deployment and no written procedure required it, so one server missed the update. The code smell involved was Dead Code, because old, unused code had been left in the system and was later triggered by accident.

**Q3:** State, in order, the four historical approaches to reducing implementation failure, and give the key idea of each.

**A3:** Structured programming (clear control flow); procedural design principles (modular procedures); early object-oriented guidelines (encapsulation, inheritance and polymorphism); and the SOLID principles (five design principles for object-oriented code).

---

### 3.2 Foundations Used Throughout: Cohesion, Coupling, Abstraction, Polymorphism and Dependency Injection

> ⚠️ **Flag (added prerequisite section):** The slides use the terms below (for example "high cohesion", "coupling", "abstractions", "injected via constructor", "mock") without defining them. This section was inserted before the SOLID principles so that every later explanation is understandable on its own. It is a supplement to the lecture, kept minimal, and it does not reorder any slide content.

**Encapsulation** means a class keeps its data private and exposes only the operations others need, so its internals can change without breaking its users.

**Cohesion** is how strongly the parts of one class or method belong together. A class is *highly cohesive* when all its methods and fields serve one purpose (for example, a class that only calculates tax). It is *low in cohesion* when it mixes unrelated jobs (for example, a class that both prints invoices and sends e-mail).

**Coupling** is how strongly one class depends on the details of another. *Tight coupling* means a change in one class forces changes in the other (for example, code that calls `new EmailService()` directly cannot use anything else without editing). *Loose coupling* means the classes interact only through a small, stable contract. Good design aims for high cohesion and low coupling.

**Abstraction (interface and abstract class).** An *interface* lists methods that any implementing class must provide, without saying how. An *abstract class* is a partially built base class that other classes extend. Code that depends on the interface or abstract class (the *abstraction*) works with any class that implements or extends it, without knowing which one.

**Polymorphism** means calling the same method on a variable of an abstraction type and getting the behaviour of whichever concrete class the object actually is. With `Notifier n = new SmsNotifier(); n.send(...)`, the SMS version of `send` runs; with `new EmailNotifier()` the e-mail version runs. The calling code is identical.

**Dependency injection (constructor injection).** A class receives the objects it depends on from outside, usually as constructor arguments, instead of creating them itself with `new`:

```java
class OrderProcessor {
    private final Notifier notifier;                // depends on the abstraction
    OrderProcessor(Notifier notifier) {             // supplied from outside
        this.notifier = notifier;
    }
}
```

**Test double (mock).** A stand-in object used in a unit test in place of a real dependency, so the class under test can be checked in isolation (for example, a fake notifier that records messages instead of sending e-mail). A class that creates its own dependencies with `new` cannot have them replaced this way.

**Possible Exam Angles (with model answers)**

**Q1:** Class `ReportManager` has methods `calculateTotals()`, `printPdf()` and `emailToClient()`. Is its cohesion high or low, and why?

**A1:** Low. The three methods do unrelated jobs (calculation, document output and messaging), so the class does not have one purpose. A change to the PDF layout, a change to e-mail sending and a change to the calculation rules would all force edits to this one class.

**Q2:** Explain why `class A { private B b = new B(); }` makes A hard to unit-test in isolation.

**A2:** A creates its own concrete B, so it is tightly coupled to it. In a test there is no way to substitute a fake B, so A can only be tested together with the real B (and whatever B does, such as sending real e-mail). Receiving an abstraction through the constructor would let a test double be supplied.

---

### 3.3 SOLID Principles: Overview

SOLID is an acronym for five principles that guide good implementation of object-oriented code. Each letter stands for one principle:

| Letter | Principle | One-line rule |
|---|---|---|
| **S** | Single Responsibility Principle (SRP) | A class should have one, and only one, reason to change (high cohesion). |
| **O** | Open/Closed Principle (OCP) | A class should be open for extension and closed for modification. |
| **L** | Liskov Substitution Principle (LSP) | Subtypes must be substitutable for their base types. |
| **I** | Interface Segregation Principle (ISP) | Clients should not be forced to depend on methods they do not use. |
| **D** | Dependency Inversion Principle (DIP) | High-level modules should not depend on low-level modules; both should depend on abstractions. |

The five principles are covered one at a time in Sections 3.4 (Single Responsibility Principle) to 3.8 (Dependency Inversion Principle) and compared side by side in Section 3.9 (SOLID Principles Compared). Each one is presented the same way below: a rule, the reasons violating it is bad, a "bad" code example, and a "better" version of the same code.

**Possible Exam Angles (with model answers)**

**Q1:** Write out what each letter of SOLID stands for.

**A1:** S: Single Responsibility Principle. O: Open/Closed Principle. L: Liskov Substitution Principle. I: Interface Segregation Principle. D: Dependency Inversion Principle.

**Q2:** Match each rule to its principle: (a) "depend on abstractions, not concrete classes", (b) "a class has one reason to change", (c) "add features by extending, not editing", (d) "child classes must work wherever the parent works", (e) "many small interfaces beat one big one".

**A2:** (a) Dependency Inversion (DIP); (b) Single Responsibility (SRP); (c) Open/Closed (OCP); (d) Liskov Substitution (LSP); (e) Interface Segregation (ISP).

---

### 3.4 Single Responsibility Principle (SRP)

**The rule.** A class should have one, and only one, reason to change. That is another way of saying it should be highly cohesive (see Section 3.2, Foundations Used Throughout). A class has a "reason to change" for each distinct job it performs: if invoice layout, e-mail wording and order rules all live in one class, then three different kinds of requirement change all force edits to that class.

**Why violating it is bad.** A class with too many responsibilities makes the codebase harder to read and understand. It also makes the codebase fragile: it is harder to maintain and test, because a change for one responsibility can break another.

**The bad example.** `OrderService` does three jobs: it creates orders, prints invoices and sends e-mail confirmations.

```java
class Order {
    // order details...
}

class OrderService {
    void createOrder(Order o) {
        System.out.println("Order created");
    }

    void printInvoice(Order o) {
        System.out.println("Invoice printed");
    }

    void sendEmailConfirmation(Order o) {
        System.out.println("Email confirmation sent");
    }
}
```

**Implementation Analysis.** `Order` is a plain holder for order data. `OrderService` groups three unrelated behaviours behind one class name. Each method takes the `Order` as its parameter, which shows that none of them needs the others. They only happen to be about "orders". That "about the same noun" grouping is the mistake, because the behaviours change for different reasons (business rules, document layout, messaging).

**The better version.** Each responsibility gets its own class.

```java
class Order {
    // order details...
}

class OrderService {
    void createOrder(Order o) {
        System.out.println("Order created");
    }
}

class InvoicePrinter {
    void printInvoice(Order o) {
        System.out.println("Invoice printed");
    }
}

class EmailService {
    void sendEmailConfirmation(Order o) {
        System.out.println("Email confirmation sent");
    }
}
```

**Implementation Analysis.** `OrderService` now only creates orders. Invoice output moved to `InvoicePrinter` and e-mail to `EmailService`. Changing the invoice layout now touches one small class, and each class can be tested and reused by itself.

**Benefits of SRP:**

- The code is easier to understand, debug and refactor.
- The class is easier to maintain.
- Changes are isolated: if you break something, you break one thing (or fewer), not an entire system.
- The class is more reusable.

A memorable way to hold SRP in mind: "Just because you can, doesn't mean you should": a class can hold every method, but it should not.

**Lab Sheet 3, Task 11 (SRP): identify and correct violations by splitting responsibilities into smaller, focused classes or methods.**

The provided project has three clear SRP violations.

*Violation 1: `EmployeeService` (original) did at least seven unrelated things.* It stored employees, printed payslips, exported CSV, sent promotion e-mail, drew organisation charts (all through one five-method interface), plus formatted names and looped over employees printing to the console.

```java
// BEFORE (excerpt)
public class EmployeeService implements EmployeeOperations {
    public void saveEmployee(Object e) { /* ... */ }
    public void printPayslip(Object e) { throw new UnsupportedOperationException(); }
    public void exportToCsv(Object e) { /* ... */ }
    public void sendPromotionEmail(Object e) { /* ... */ }
    public void drawOrgChart(Object e) { /* ... bizarre GUI stub */ }

    public void processEverything(List employees) { /* ... see Part C, Item 1 for the full body ... */ }
    public String formatEmployeeName1(String n) { /* ... */ }
    public String formatEmployeeName2(String n) { /* ... */ }
}
```

*Fix:* the five operations were split into separate small interfaces (Section 3.7, Interface Segregation Principle covers this), name formatting moved into one dedicated class, and the service keeps only what it genuinely supports:

```java
// AFTER: name formatting has exactly one home
public final class NameFormatter {
    private NameFormatter() {
        throw new AssertionError("NameFormatter is a utility class and must not be instantiated");
    }
    public static String format(String name) {
        if (name == null) { return ""; }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) { return ""; }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}

// AFTER: the service is reduced to a small, focused role
public class EmployeeService implements EmployeeStorage, EmployeeExporter, PromotionNotifier { /* ... */ }
```

*Violation 2: `Department` (original) mixed identity data, level naming and describing employees.* It held public fields, translated a numeric code to a level name in two separate methods, and built a sentence about an employee. *Fix:* the level naming moved to a `DepartmentLevel` enum, and the employee sentence moved to `Employee` (Section 3.11, The Common Code Smells covers Feature Envy). `Department` now only represents a department.

*Violation 3: `Utility` (original) mixed name formatting with console printing.* *Fix:* formatting moved to `NameFormatter`; the unused console-dump method was deleted.

**Possible Exam Angles (with model answers)**

**Q1:** Identify the SRP violation and fix it:
```java
class Employee {
    String name;
    double salary;
    void calculatePay() { /* ... */ }
    void saveToDatabase() { /* ... */ }
    void generateReport() { /* ... */ }
}
```

**A1:** `Employee` has three reasons to change: pay rules (`calculatePay`), persistence (`saveToDatabase`) and reporting (`generateReport`). Split it so `Employee` holds only employee data, and give each other job its own class, for example `PayCalculator`, `EmployeeRepository` and `ReportGenerator`, each taking an `Employee`.

**Q2:** A team says: "Our `UserManager` class has 40 methods, but they are all about users, so it follows SRP." Is the team right?

**A2:** No. SRP is about reasons to change, not about a shared noun. If `UserManager` handles authentication, profile storage, e-mail notices and report exports, it changes for four unrelated reasons. Being "about users" does not make it cohesive; it is a God Class (a class that has too many responsibilities; see Section 3.11, The Common Code Smells).

**Q3:** Give two benefits of applying SRP.

**A3:** Any two of: code is easier to understand, debug and refactor; the class is easier to maintain; changes are isolated so a mistake breaks one thing rather than the whole system; the class is more reusable.

---

### 3.5 Open/Closed Principle (OCP)

**The rule.** A class should be open for extension and closed for modification. *Modification* means changing the code of an existing class. *Extension* means adding new functionality. You should be able to add new functionality without touching the existing code of the class. This is usually done with interfaces and abstract classes (Section 3.2, Foundations Used Throughout).

**Why violating it is bad.** If existing code has to be edited to make any change, the edits risk stability problems in code that already worked. The codebase becomes fragile, and harder to maintain and test.

**The bad example.** The discount for each customer type is decided by an if/else chain, so a new customer type means editing `DiscountService`.

```java
class Order {
    String customerType;

    public Order(String customerType) {
        this.customerType = customerType;
    }
}

class DiscountService {
    double getDiscount(Order o) {
        if (o.customerType.equals("REGULAR")) {
            return 0.05;
        } else if (o.customerType.equals("PREMIUM")) {
            return 0.10;
        }
        // ❌ Adding a new customer type requires modifying this method
        return 0.0;
    }
}
```

**Implementation Analysis.** The customer type is a `String` compared with `equals`. Every new type adds another `else if` branch inside `getDiscount`, so the method is modified each time. The final `return 0.0` silently gives an unknown type no discount, which hides mistakes. This pattern is also the Switch/If-Else Chains smell (a long conditional chain that decides behaviour; Section 3.11, The Common Code Smells).

**The better version.** A `DiscountPolicy` interface represents "a way of calculating a discount". Each customer type is its own class, and `Order` is given a policy.

```java
interface DiscountPolicy {
    double getDiscount();
}

class RegularCustomerDiscount implements DiscountPolicy {
    @Override
    public double getDiscount() {
        return 0.05;
    }
}

class PremiumCustomerDiscount implements DiscountPolicy {
    @Override
    public double getDiscount() {
        return 0.10;
    }
}

class Order {
    private final DiscountPolicy discountPolicy;

    public Order(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public double getDiscount() {
        return discountPolicy.getDiscount();
    }
}

// Adding a new discount policy called VIPCustomerDiscount is a breeze.
// No changes to existing code - we extend, not modify.
class VipCustomerDiscount implements DiscountPolicy {
    @Override
    public double getDiscount() {
        return 0.15;
    }
}
```

**Implementation Analysis.** `Order` depends only on the `DiscountPolicy` interface and delegates to whichever policy it was given (polymorphism, Section 3.2, Foundations Used Throughout). `@Override` makes the compiler check that each class really implements the interface method. `private final` and the constructor mean the policy is chosen once, when the order is created. Adding `VipCustomerDiscount` is a new class; no existing class is edited, so the code that already passed its tests stays untouched.

**Benefits of OCP.** Because existing code is not modified, no new bugs can be introduced into it, so the code becomes more stable over time as it keeps being tested. Being able to reuse a class that has worked for years without modifying it is clearly preferable to editing the class every time requirements change.

A memorable way to hold OCP in mind: "Open chest surgery is not needed when putting on a coat": you should be able to add something to the system without cutting open what already works.

> ⚠️ **Flag (two different `DiscountPolicy` shapes):** The OCP slides define `DiscountPolicy` with `getDiscount()` (no parameters), while the Switch/If-Else Chains slides later (Section 3.11, The Common Code Smells) define a different `DiscountPolicy` with `getDiscount(Order o)`. These are two separate examples that reuse a name; do not treat them as one interface.

**Lab Sheet 3, Task 12 (OCP): remove the hard-coded `EmailNotifier` in `EmployeeService` and use an interface so new notifiers can be added without modifying existing code.**

```java
// BEFORE
public class EmployeeService implements EmployeeOperations {
    // hard-coded dependency (DIP break), not extendable (OCP break)
    private EmailNotifier notifier = new EmailNotifier();

    public void notifyDone(String to) { notifier.send(to, "Report generated"); }
}

public class EmailNotifier {
    public void send(String to, String body) {
        System.out.println("Email sent to " + to + ": " + body);
    }
}
```

```java
// AFTER: an abstraction, and an implementation of it
public interface Notifier {
    void send(String to, String body);
}

public class EmailNotifier implements Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(EmailNotifier.class);

    @Override
    public void send(String to, String body) {
        LOG.log(Level.INFO, "Email sent to {0}: {1}", new Object[] {to, body});
    }
}
```

**Implementation Analysis.** `EmployeeService` now uses only the `Notifier` type, so it does not know or care which channel sends the message. `EmailNotifier` implements `Notifier` and logs through a logger instead of printing to the console. To support SMS, you would add a class such as the one below, and `EmployeeService` would not change at all. (`SmsNotifier` is an illustration of the principle, not a class in the project; the mechanism was checked in testing by supplying a different `Notifier` to the service.)

```java
// Illustration only: not part of the provided or refactored project
public class SmsNotifier implements Notifier {
    @Override
    public void send(String to, String body) { /* send an SMS */ }
}
```

**Possible Exam Angles (with model answers)**

**Q1:** Which SOLID principle does this violate, and how would you fix it?
```java
class AreaCalculator {
    double area(Object shape) {
        if (shape instanceof Circle c)   return Math.PI * c.r * c.r;
        else if (shape instanceof Square s) return s.side * s.side;
        return 0;
    }
}
```

**A1:** It violates the Open/Closed Principle: adding a new shape (for example a triangle) means editing `area`. Fix: define an interface `Shape { double area(); }`, let `Circle` and `Square` implement it, and have the calculator call `shape.area()`. A new shape is then a new class, with no change to existing code.

**Q2:** "Closed for modification" does not mean a class can never be edited, even to fix a bug. What does it mean?

**A2:** It means new behaviour should be added by extension (new classes that implement an interface or extend an abstract class) rather than by editing the existing, already-tested class each time requirements change. Fixing an actual defect in a class is still allowed.

**Q3:** Name one benefit of OCP for code stability.

**A3:** Because existing code is not modified when new features are added, no new bugs can be introduced into it, and the code becomes more stable over time through continued testing.

---

### 3.6 Liskov Substitution Principle (LSP)

**The rule.** Subtypes must be substitutable for their base types. Code written for the base type must keep working, without surprises, when handed any subclass.

**Why violating it is bad.** It breaks the expectations set by the parent's behaviour. The whole point of using an abstract base class is that, in the future, you can write a new subclass and insert it into existing, working, tested code. A subclass that breaks the parent's expectations defeats that.

A memorable way to hold LSP in mind: "No matter what you used to learn how to drive, you should be able to drive any car afterward."

**The bad example.** `FreeOrder` extends `Order` but makes `applyDiscount` throw. It appears among a set of subclasses (the first two are acceptable, the last is the violation):

```java
class Order {
    void applyDiscount(double percentage) {
        System.out.println("Discount applied: " + percentage + "%");
    }
}

// ✅ LSP-preserving: still applies a discount as expected
class SeasonalOrder extends Order {
    @Override
    void applyDiscount(double percentage) {
        double totalDiscount = percentage + 5; // seasonal bonus
        System.out.println("Discount applied: " + totalDiscount + "% (including seasonal bonus)");
    }
}

// ✅ LSP-preserving: still applies a discount as expected
class BulkOrder extends Order {
    @Override
    void applyDiscount(double percentage) {
        double totalDiscount = percentage + 10; // bulk bonus
        System.out.println("Discount applied: " + totalDiscount + "% (bulk discount included)");
    }
}

// ❌ LSP-violating: breaks expected behavior by throwing
class FreeOrder extends Order {
    @Override
    void applyDiscount(double percentage) {
        throw new UnsupportedOperationException("Free orders can't have discounts");
    }
}
```

The failure shows up in code that only knows about `Order`:

```java
// Call site A: works with whatever object it is handed
Order order = getOrderFromSomewhere();
order.applyDiscount(10);
```

And, at a different call site (a separate method, so the variable name does not clash):

```java
Order order = new FreeOrder();
order.applyDiscount(10); // 💥 Runtime error!
```

**Implementation Analysis.** `Order order = getOrderFromSomewhere();` is the substitution: the variable's type is the base class, but the object can be any subclass. Calling `applyDiscount(10)` is safe for `Order`, `SeasonalOrder` and `BulkOrder`, but for a `FreeOrder` it throws at run time. The compiler cannot catch this, because the method exists; only the behaviour is wrong. `@Override` shows the subclass replaces the parent's method, and `UnsupportedOperationException` is the tell-tale sign: a subclass that refuses to do what its parent promised.

> ⚠️ **Flag (the "LSP-preserving" labels on `SeasonalOrder` and `BulkOrder`):** For the quiz, follow the lecture: these two are labelled acceptable and `FreeOrder` is the violation. Read strictly, however, both also change what the caller asked for (asked for `percentage`, applied `percentage + 5` or `percentage + 10`), which alters the base class's promised behaviour. Their point in the lecture is to contrast with the unmistakable failure, a subclass that throws.

**The better version.** `FreeOrder` should not pretend to be an order that supports discounts. The discount behaviour moves to an interface, and only classes that truly support discounts implement it.

```java
interface Discountable {
    void applyDiscount(double percentage);
}

class Order implements Discountable {
    @Override
    public void applyDiscount(double percentage) {
        System.out.println("Discount applied: " + percentage + "%");
    }
}

class FreeOrder {
    // No discount-related behavior
    void markAsFree() {
        System.out.println("Order is free");
    }
}
```

**Implementation Analysis.** `FreeOrder` is no longer a subtype of anything that promises discounts, so there is nothing for it to break. Any code that deals with `Discountable` can safely assume `applyDiscount` behaves the same way for every implementor.

**Lab Sheet 3, Task 13 (LSP): ensure subclasses can be used interchangeably with their base class without breaking behaviour.**

The provided `ContractEmployee` extends `Employee` and breaks the base class's contract in five ways.

```java
// BEFORE
public class ContractEmployee extends Employee {
    private final Random rnd = new Random();

    public String tostrng() { throw new RuntimeException("Not supported for contractors"); }

    @Override
    public void setname(String n) {
        if (n == null || n.length() < 5) {
            throw new IllegalArgumentException("Contractor name must be at least 5 characters");
        }
        super.setname(n);
    }

    @Override public boolean equals(Object o) { return false; }
    @Override public int hashCode() { return rnd.nextInt(); }
    @Override public String toString() { return null; }
}
```

The five problems and why each breaks substitution:

1. `tostrng()` always throws: a method that fails whenever it is called.
2. `setname` rejects names shorter than 5 characters, although `Employee` accepts them. This is a *stronger precondition* than the parent's; code that works with `Employee` fails with a contractor.
3. `equals` always returns `false`, so even `x.equals(x)` is false.
4. `hashCode` returns a random number each call, so the same object cannot be found in a hash-based collection.
5. `toString` returns `null` instead of text.

```java
// AFTER: nothing overridden that changes the contract; only the real difference remains
public class ContractEmployee extends Employee {
    public ContractEmployee(String name) { super(name); }

    @Override
    public boolean isTemporary() { return true; }
}
```

**Implementation Analysis.** All five broken overrides were removed. The one genuine difference between a contractor and a regular employee, being temporary, is expressed as a method (`isTemporary()`), which replaces the old boolean flag `isTemp`. A `ContractEmployee` now behaves like an `Employee` everywhere: setting a short name works, `equals` and `hashCode` are consistent, and it can be stored in a `HashSet`.

**Possible Exam Angles (with model answers)**

**Q1:** Does this violate LSP? Explain.
```java
class Bird { void fly() { /* flies */ } }
class Penguin extends Bird {
    @Override void fly() { throw new UnsupportedOperationException("Penguins can't fly"); }
}
```

**A1:** Yes. Code written for `Bird` calls `fly()` expecting it to work; substituting a `Penguin` makes that call throw at run time. The subclass refuses a behaviour its parent promised. A fix is to move flying to a separate abstraction (for example a `Flyable` interface) that only flying birds implement, instead of having every bird inherit `fly()`.

**Q2:** A subclass overrides a method and adds a stricter input check than the parent has (rejecting inputs the parent accepts). Which principle is broken, and why?

**A2:** The Liskov Substitution Principle. The subclass strengthens the parent's precondition, so code that passes a valid input to the base type can suddenly fail when a subclass is substituted, breaking the expectation that a subtype behaves like its base type.

**Q3:** How can a compiler-clean program still violate LSP?

**A3:** LSP is about behaviour, not method signatures. A subclass can have exactly the right method names and types (so it compiles), yet throw exceptions, return `null`, or change results in ways callers of the base type do not expect. These faults appear only at run time.

---

### 3.7 Interface Segregation Principle (ISP)

**The rule.** Clients should not be forced to depend on methods they do not use. Many client-specific interfaces are better than one general-purpose interface.

**Why violating it is bad.** Some implementing classes have no need for some of the methods and end up implementing them as empty or exception-throwing stubs. Interfaces with additional, useless methods are called *fat interfaces*, and they lead to more coupling.

A memorable way to hold ISP in mind: "Don't force the client to depend on things they don't use."

**The bad example.** One interface, `OrderOperations`, forces a digital order to implement shipping and tracking.

```java
interface OrderOperations {
    void createOrder();
    void cancelOrder();
    void shipOrder();
    void trackOrder();
}

class DigitalOrder implements OrderOperations {

    @Override
    public void createOrder() {
        System.out.println("Digital order created");
    }

    @Override
    public void cancelOrder() {
        System.out.println("Digital order cancelled");
    }

    @Override
    public void shipOrder() {
        // ❌ Digital products aren't shipped!
        throw new UnsupportedOperationException("Digital orders cannot be shipped");
    }

    @Override
    public void trackOrder() {
        // ❌ Tracking makes no sense for digital downloads
        throw new UnsupportedOperationException("No tracking available for digital orders");
    }
}
```

**Implementation Analysis.** A class that implements an interface must provide every method. `DigitalOrder` has no physical shipment, so its `shipOrder` and `trackOrder` can only throw. Anyone holding an `OrderOperations` reference is offered methods that fail at run time.

**The better version.** The interface is split by what clients need: creation on one side, shipping on the other.

```java
interface OrderCreation {
    void createOrder();
    void cancelOrder();
}

interface Shippable {
    void shipOrder();
    void trackOrder();
}

class DigitalOrder implements OrderCreation {

    @Override
    public void createOrder() {
        System.out.println("Digital order created");
    }

    @Override
    public void cancelOrder() {
        System.out.println("Digital order cancelled");
    }
}

class PhysicalOrder implements OrderCreation, Shippable {

    @Override
    public void createOrder() {
        System.out.println("Physical order created");
    }

    @Override
    public void cancelOrder() {
        System.out.println("Physical order cancelled");
    }

    @Override
    public void shipOrder() {
        System.out.println("Physical order shipped");
    }

    @Override
    public void trackOrder() {
        System.out.println("Tracking physical order");
    }
}
```

**Implementation Analysis.** `DigitalOrder` implements only `OrderCreation`, so it has nothing to stub. `PhysicalOrder` implements two interfaces (Java lets a class implement several), so it supports exactly the behaviours it really has. Code that only needs shipping depends only on `Shippable`.

**Lab Sheet 3, Task 14 (ISP): avoid large, multipurpose interfaces by splitting them into smaller, specific ones.**

```java
// BEFORE: one fat interface with an unrelated UI concern
public interface EmployeeOperations {
    void saveEmployee(Object e);
    void printPayslip(Object e);
    void exportToCsv(Object e);
    void sendPromotionEmail(Object e);
    void drawOrgChart(Object e); // unrelated UI concern
}

// and EmployeeService forced to implement all of it:
public void printPayslip(Object e) { throw new UnsupportedOperationException(); } // breaks ISP
public void drawOrgChart(Object e) { /* ... bizarre GUI stub */ }
```

```java
// AFTER: five small role interfaces (each is in its own file; condensed to one line each here)
public interface EmployeeStorage    { void saveEmployee(Employee employee); }
public interface EmployeeExporter   { void exportToCsv(Employee employee); }
public interface PayslipPrinter     { void printPayslip(Employee employee); }
public interface PromotionNotifier  { void sendPromotionEmail(Employee employee); }
public interface OrgChartRenderer   { void drawOrgChart(Employee employee); }

// EmployeeService implements only the three it genuinely supports
public class EmployeeService implements EmployeeStorage, EmployeeExporter, PromotionNotifier { /* ... */ }
```

**Implementation Analysis.** The service no longer contains the method that threw `UnsupportedOperationException` (`printPayslip`) or the GUI stub (`drawOrgChart`). `PayslipPrinter` and `OrgChartRenderer` exist so that a class that really does those jobs can implement them; nothing in this small project does yet. The parameter type was also tightened from `Object` to `Employee`, so callers cannot pass anything unrelated. (The three remaining methods keep the original empty-stub behaviour.)

**Possible Exam Angles (with model answers)**

**Q1:** Identify the SOLID violation and fix it.
```java
interface Worker { void work(); void eat(); }
class Robot implements Worker {
    public void work() { /* ... */ }
    public void eat()  { throw new UnsupportedOperationException(); }
}
```

**A1:** It violates the Interface Segregation Principle: `Robot` is forced to implement `eat()`, which it cannot do. Fix: split the interface into `Workable { void work(); }` and `Feedable { void eat(); }`. `Robot` implements only `Workable`; a human worker implements both.

**Q2:** Both LSP and ISP violations can show `UnsupportedOperationException`. How do you tell which principle a given snippet violates?

**A2:** Look at what the class is doing. If a class *extends a parent class* and overrides an inherited method to throw, it breaks LSP (it cannot stand in for its parent). If a class *implements a large interface* and throws from the methods it does not need, it breaks ISP (the interface forced methods on it). The fixes differ too: for LSP, stop inheriting the unsupported behaviour (for example, move it to a separate interface); for ISP, split the interface.

**Q3:** Why do fat interfaces increase coupling?

**A3:** Every class that implements or uses the interface becomes tied to all its methods, including ones it does not need. A change to any method affects every implementor and client, even those that never used it.

---

### 3.8 Dependency Inversion Principle (DIP)

**The rule.** High-level modules should not depend upon low-level modules. Both should depend upon abstractions. A *high-level module* holds the main business logic (for example, processing an order); a *low-level module* does a detailed job for it (for example, sending an e-mail).

**Why violating it is bad.** The high-level class is tightly coupled to one concrete class. Swapping to another concrete class requires modifying the code, which is an OCP violation (Section 3.5, Open/Closed Principle). Testing also becomes harder, because it is harder to mock a module (Section 3.2, Foundations Used Throughout).

A memorable way to hold DIP in mind: "You don't have to learn how to wire things up if you need to turn on the lights. You use a socket for that." The socket is the abstraction.

**The bad example.** `OrderProcessor` creates its own `EmailService` with `new`.

```java
class EmailService {
    public void sendEmail(String message) {
        System.out.println("Sending email: " + message);
    }
}

class OrderProcessor {
    // ❌ High-level module depends directly on a low-level module
    private EmailService emailService = new EmailService();

    public void processOrder() {
        System.out.println("Processing order...");
        emailService.sendEmail("Order processed");
    }
}
```

**Implementation Analysis.** `new EmailService()` fixes the choice of notification channel inside the high-level class. To use SMS instead, `OrderProcessor` has to be edited. A test cannot substitute a fake, so testing `processOrder` sends a real message.

**The better version.** An interface sits between them, and the dependency is injected through the constructor.

```java
interface NotificationService {
    void sendNotification(String message);
}

class EmailService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending email: " + message);
    }
}

class SMSService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("Sending SMS: " + message);
    }
}

class OrderProcessor {
    private final NotificationService notificationService;

    // ✅ Dependency injected via constructor
    public OrderProcessor(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void processOrder() {
        System.out.println("Processing order...");
        notificationService.sendNotification("Order processed");
    }
}
```

**Implementation Analysis.** Both `EmailService` and `SMSService` now depend on the abstraction by implementing it, and `OrderProcessor` depends on the abstraction by using it: this is the "inversion", since the high-level class no longer points at a low-level class. The `final` field is assigned once in the constructor, so the collaborator cannot be swapped by accident. Which service is used is decided by whoever creates the `OrderProcessor`, for example `new OrderProcessor(new SMSService())`, and a test can pass a fake.

**Lab Sheet 3, Task 15 (DIP): make high-level modules depend on abstractions; depend on abstractions for Notifier and Repository in `EmployeeService`, injected via the constructor.**

```java
// BEFORE: EmployeeService creates its own concrete collaborators
public class EmployeeService implements EmployeeOperations {
    private EmailNotifier notifier = new EmailNotifier();
    private Repository repository = new Repository();
    // ...
}
```

```java
// AFTER: both collaborators are abstractions, supplied through the constructor
public class EmployeeService implements EmployeeStorage, EmployeeExporter, PromotionNotifier {
    private final Notifier notifier;
    private final EmployeeRepository repository;

    public EmployeeService(Notifier notifier, EmployeeRepository repository) {
        this.notifier = Objects.requireNonNull(notifier, "notifier");
        this.repository = Objects.requireNonNull(repository, "repository");
    }
    // ...
}

// The abstraction for data access
public interface EmployeeRepository {
    List<Employee> loadEmployees();
}

// Only Main (the entry point) names the concrete classes and wires them together
EmployeeRepository repository = new InMemoryEmployeeRepository();
EmployeeService service = new EmployeeService(new EmailNotifier(), repository);
```

**Implementation Analysis.** The two `new` calls left `EmployeeService` entirely; it now imports only `Notifier` and `EmployeeRepository`. `Objects.requireNonNull` fails immediately, with a clear message, if a caller forgets to supply a dependency, instead of failing later with an obscure error. The original `Repository` class became `InMemoryEmployeeRepository`, an implementation of the new `EmployeeRepository` abstraction. In testing, passing a fake `Notifier` and a fake `EmployeeRepository` to the service worked with no change to the service, which is exactly what the principle promises.

**Possible Exam Angles (with model answers)**

**Q1:** Identify the SOLID violation and fix it.
```java
class ReportService {
    private MySqlDatabase db = new MySqlDatabase();
    void save(Report r) { db.insert(r); }
}
```

**A1:** It violates the Dependency Inversion Principle: the high-level `ReportService` creates and depends on the concrete low-level `MySqlDatabase`. Fix: define an interface such as `ReportStore { void insert(Report r); }`, make `MySqlDatabase` implement it, and give `ReportService` a `ReportStore` through its constructor. Switching database or testing with a fake then needs no change to `ReportService`.

**Q2:** Explain how a DIP violation leads to an OCP violation.

**A2:** If a high-level class hard-codes a concrete low-level class, then supporting a different low-level implementation means editing the high-level class's source code. That is modification of existing code to add a feature, which is exactly what OCP forbids. Depending on an abstraction lets a new implementation be added by extension only.

**Q3:** In "Dependency Inversion", what is being inverted?

**A3:** The direction of the source-code dependency. Without DIP, the high-level module depends on the low-level module. With DIP, both depend on an abstraction, so the high-level module no longer points at the low-level detail.

---

### 3.9 SOLID Principles Compared

The five principles side by side, framed for the two things a quiz asks: recognising a violation in a code snippet, and stating the fix.

| Principle | What the violating code looks like | Typical fix | What it protects |
|---|---|---|---|
| **SRP** | One class with methods for unrelated jobs (create + print + e-mail; payment + invoice + marketing) | Split into one class per responsibility | Cohesion; changes stay isolated |
| **OCP** | An if/else or switch on a type or code that must be edited for every new case | Interface or abstract class plus one implementing class per case | Existing, tested code stays untouched |
| **LSP** | A subclass that overrides an inherited method to throw, return `null`, refuse inputs the parent accepts, or otherwise break the parent's promise | Do not inherit the unsupported behaviour (move it to a separate interface); keep subclass behaviour compatible | Code written for the base type keeps working |
| **ISP** | A class implementing a big interface with methods that throw or are empty stubs | Split the interface into small, client-specific ones | Clients depend only on what they use; lower coupling |
| **DIP** | `new ConcreteClass()` inside a high-level class; the field's type is the concrete class | Depend on an interface; inject the implementation through the constructor | Swappable and testable collaborators |

**How the principles connect.** They reinforce one another rather than standing alone:

- A DIP violation forces an OCP violation: if a high-level class hard-codes a concrete class, switching implementations means editing it (Section 3.8, Dependency Inversion Principle).
- The fix for an OCP violation (an interface with several implementations) is also the mechanism DIP relies on (Section 3.5, Open/Closed Principle).
- Both LSP and ISP violations can show up as `UnsupportedOperationException`. The difference is the cause: a subclass overriding a *parent class's* method (LSP), or a class forced to implement *interface* methods it does not need (ISP) (Section 3.7, Interface Segregation Principle).
- SRP is the principle behind the God Class smell (one class with too many responsibilities) and the Divergent Change smell (one class edited for many unrelated reasons), both described in Section 3.11 (The Common Code Smells).

**Quick recognition guide for a snippet-based quiz:**

- `new` of a concrete class inside another class, stored in a field: **DIP**.
- if / else if / switch on a type string or code, choosing behaviour: **OCP** (and the Switch/If-Else Chains smell).
- `throw new UnsupportedOperationException(...)` in an override of a *superclass* method: **LSP**.
- `throw new UnsupportedOperationException(...)` in a method required by an *interface*: **ISP**.
- A class whose methods do clearly different jobs: **SRP**.

**Possible Exam Angles (with model answers)**

**Q1:** List every SOLID violation you can find.
```java
class OrderManager {
    private EmailService email = new EmailService();
    double discount(String type) {
        if (type.equals("VIP")) return 0.2;
        else if (type.equals("STAFF")) return 0.3;
        return 0;
    }
    void saveToDb(Order o) { /* ... */ }
    void printInvoice(Order o) { /* ... */ }
}
```

**A1:** (1) SRP: `OrderManager` handles discounts, persistence and invoice printing. (2) OCP: the if/else chain in `discount` must be edited for every new customer type. (3) DIP: `OrderManager` creates a concrete `EmailService` with `new`. Fixes: split the responsibilities into separate classes; replace the chain with a `DiscountPolicy` interface and one class per type; inject a notifier interface through the constructor.

**Q2:** A subclass `ReadOnlyList extends ArrayList` overrides `add()` to throw `UnsupportedOperationException`. Which principle is violated, and what is a better design?

**A2:** Liskov Substitution: code that expects an `ArrayList` will call `add()` and fail at run time. A better design does not make it a subtype of a mutable list; expose a read-only view through a separate, smaller interface that has no `add` method.

**Q3:** Explain why the principles "reinforce" one another, using DIP and OCP as the example.

**A3:** If `A` creates a concrete `B` (DIP violation), replacing `B` with a different class means editing `A` (OCP violation). Making `A` depend on an interface `I` that `B` implements (DIP fix) means a new implementation is added as a new class, leaving `A` unchanged (OCP satisfied).

---

### 3.10 Code Smells: What They Are

**Definition.** A code smell is a symptom in code that may indicate a deeper problem. Key points:

- Even with the SOLID principles in mind, issues can still arise during implementation.
- Code smells are symptoms, not necessarily bugs. A smell does not necessarily mean the code is wrong or broken; the program may run perfectly.
- Detecting smells early prevents *technical debt*: the accumulated cost of quick or messy solutions, which must eventually be "paid back" as extra effort whenever the code has to be changed, understood or fixed.

**Required reading.** Martin Fowler's article on code smells (the source of the concept), and the *Code Smells* catalogue on refactoring.guru, which is a **mandatory reference** for this module. The questions in this chapter use that catalogue's names and groupings. It arranges smells into five groups:

| Group (refactoring.guru) | Idea | Smells on the module's list of 13 | Other smells from that group named in this chapter |
|---|---|---|---|
| **Bloaters** | Code, methods and classes that have grown so large they are hard to work with | Long Method, Large Class, Primitive Obsession, Long Parameter List, Data Clumps | none |
| **Object-Orientation Abusers** | Incomplete or incorrect use of object-oriented principles | Switch/If-Else Chains, Temporary Field | Alternative Classes with Different Interfaces |
| **Change Preventers** | A change in one place forces many changes elsewhere | Divergent Change, Shotgun Surgery | none |
| **Dispensables** | Something pointless whose absence would make the code cleaner | Duplicate Code, Dead Code | none |
| **Couplers** | Excessive coupling between classes, or excessive delegation | Feature Envy, Message Chains, Middle Man | Inappropriate Intimacy |

The module's list of common smells has 13 entries, in this order: Long Method; Large Class (God Class); Primitive Obsession and Data Clumps; Long Parameter List; Switch/If-Else Chains; Shotgun Surgery; Divergent Change; Duplicate Code; Dead Code; Temporary Field; Middle Man; Feature Envy; and Message Chains (Law of Demeter Violation). Primitive Obsession and Data Clumps share one entry but are two separate smells, so the 13 entries name 14 distinct smells. Section 3.11 (The Common Code Smells) covers them in that order.

> ⚠️ **Flag (grouping is a supplement):** The five groups and the group of each smell come from refactoring.guru, the mandatory reference, not from Lecture 3's slides, which list the smells flat. Use the groups as a memory aid; the slides' order is the order of Section 3.11 (The Common Code Smells).

**A smell-spotting example.** The method `addAll` from an abstract collection class contains several smells at once. The code is reproduced below with line numbers, and the smells are located in the table that follows it.

```java
 1  public abstract class AbstractCollection implements Collection {
 2      public void addAll(AbstractCollection c) {
 3          if (c instanceof Set) {
 4              Set s = (Set)c;
 5              for (int i=0; i < s.size(); i++) {
 6                  if (!contains(s.getElementAt(i))) {
 7                      add(s.getElementAt(i));
 8                  }
 9              }
10          } else if (c instanceof List) {
11              List l = (List)c;
12              for (int i=0; i < l.size(); i++) {
13                  if (!contains(l.get(i))) {
14                      add(l.get(i));
15                  }
16              }
17          } else if (c instanceof Map) {
18              Map m = (Map)c;
19              for (int i=0; i<m.size(); i++)
20                  add(m.keys[i], m.values[i]);
21          }
22      }
23  }
```

| Smell label | Lines outlined | Why it is a smell here |
|---|---|---|
| **Long Method** | 2–22 (the whole `addAll` method) | One method handles three different kinds of collection |
| **Switch Statement** | 3, 10 and 17 (the three `instanceof` tests) | Behaviour is chosen by testing the object's type; a new collection type means editing this method |
| **Duplicated Code** (pink outlines) | 5–9 and 12–16 (the two `for` loops) | The same "loop, skip if already contained, add" logic appears twice |
| **Duplicated Code** (blue outline) | only the `!contains` on line 6 | The same membership test is written again on line 13, but that repeat is not outlined |
| **Alternative Classes with Different Interfaces** | `s.getElementAt(i)` on line 6 versus `l.get(i)` on line 13 (the same expressions also appear on lines 7 and 14, which are not outlined) | `Set` and `List` offer the same capability (get the element at position `i`) under different method names, forcing separate code for each |
| **Inappropriate Intimacy** | `m.keys[i], m.values[i]` on line 20 | The method reaches directly into the internal arrays of `Map` instead of using an operation `Map` exposes |

> ⚠️ **Flag (two smells appear only in this figure):** *Alternative Classes with Different Interfaces* and *Inappropriate Intimacy* are labelled on the slide but are not among the lecture's list of 13 common smells, and the slides never define them. They are defined here so the figure makes sense. **Alternative Classes with Different Interfaces** (checked against refactoring.guru): two classes perform identical functions but have different method names, usually because the author of one did not know the other existed. Fixes: Rename Methods so they match; Move Method, Add Parameter and Parameterize Method to make signatures and implementation the same; and, if only part of the functionality is duplicated, Extract Superclass so the existing classes become subclasses. Afterwards one of the two classes can often be deleted. **Inappropriate Intimacy** (checked against refactoring.guru): one class uses the internal fields and methods of another class. Fixes include Move Method and Move Field, or Extract Class with Hide Delegate; and if the classes depend on each other, Change Bidirectional Association to Unidirectional.

**Possible Exam Angles (with model answers)**

**Q1:** "This method compiles and passes every test, so it has no code smell." Evaluate the statement.

**A1:** It is wrong. A code smell is a symptom that may indicate a deeper design or implementation problem; it is not a bug. Code can be fully correct and still be duplicated, over-long or tightly coupled, which makes it costly to change later. Smells matter because they predict future difficulty and technical debt, not because they cause failures today.

**Q2:** What is technical debt, and how does detecting code smells early help?

**A2:** Technical debt is the extra future cost created by quick or messy implementation choices: like a loan, it must be paid back later as extra time spent understanding, changing and fixing the code. Detecting smells early lets the team fix the structural problems while they are small, before the debt grows.

**Q3:** From the `addAll` example, which smell is the `instanceof` chain, and which principle does it break?

**A3:** It is the Switch Statement (Switch/If-Else Chain) smell. It breaks the Open/Closed Principle, because adding a new collection type requires editing `addAll`.

---

### 3.11 The Common Code Smells

Each smell below has a definition, the reason it is bad, a bad example and a better version. Lab Sheet 3 Part C asks you to identify and fix eight of them in the provided project.

> ⚠️ **Flag (two separate numbered lists in the lab sheet):** Lab Sheet 3 contains two separately numbered lists. Part A and Part B number their tasks continuously (Tasks 1–15), but Part C starts again at 1 with its own list of eight smells. To avoid confusion, Part C answers below are labelled "Lab Sheet 3, Part C, Item N" rather than as tasks. The lab covers only eight of the 13 entries on the lecture's smell list: it never asks about Long Parameter List, Shotgun Surgery, Divergent Change, Temporary Field or Middle Man, so learn those from the lecture examples.

> ⚠️ **Flag (how the standard fix is named):** The lecture shows "better" code for each smell but does not name the refactoring technique used. Technique names given here (Extract Method, Extract Class and so on) are refactoring.guru's, and are explained fully in Week 4's guide (Best Practices and Refactoring Techniques). One line is enough for now: a refactoring changes a program's internal structure to make it easier to understand and cheaper to change, without changing what it does.

**Smell 1 — Long Method**

*Definition:* a method that is too long and tries to do too much. *Why bad:* it is hard to read and maintain, and any change risks breaking multiple unrelated steps. *Standard fix:* Extract Method (move a coherent group of statements into its own well-named method).

Bad example:

```java
void processOrder(Order o) {
    validateOrder(o);
    calculateDiscount(o);
    generateInvoice(o);
    sendEmail(o);
    updateInventory(o);
    recordTransaction(o);
    notifyWarehouse(o);
}
```

Better version:

```java
void processOrder(Order o) {
    validateOrder(o);
    applyDiscount(o);
    finalizeOrder(o);
}

void finalizeOrder(Order o) {
    generateInvoice(o);
    sendEmail(o);
    updateInventory(o);
    notifyWarehouse(o);
}
```

*Implementation Analysis:* the seven calls are grouped into three steps of a higher-level story (validate, price, finalise). The top-level method now reads like an outline, and the details live in small methods named for what they do. Smaller, focused methods improve readability and maintainability.

> ⚠️ **Flag (the slide's two versions are not identical):** The "better" version renames `calculateDiscount` to `applyDiscount` and leaves out `recordTransaction`. Treat this as an illustration of the structure, not as an exact before/after pair; a real extraction must keep every step.

**Lab Sheet 3, Part C, Item 1 (Long Method).** In the provided project the culprit is `EmployeeService.processEverything`, which mixes name formatting, printing, a call chain and a dead branch in one method.

```java
// BEFORE
public void processEverything(List employees) {
    for (Object o : employees) {
        com.sliit.badcode.Employee e = (com.sliit.badcode.Employee) o;
        String nm = e.name == null ? "" : e.name.trim();
        if (!nm.isEmpty()) nm = nm.substring(0,1).toUpperCase() + nm.substring(1).toLowerCase();
        System.out.println("EMP: " + nm);
    }

    // message chain
    if (getRepository() != null) {
        getRepository().getDataSource().getConnection().toString();
    }

    // dead code
    if (false) {
        System.out.println("Never runs");
    }
}
```

```java
// AFTER
public void processEmployees(List<Employee> employees) {
    for (Employee employee : employees) {
        logDisplayName(employee);
    }
}

private void logDisplayName(Employee employee) {
    LOG.log(Level.INFO, "EMP: {0}", NameFormatter.format(employee.getName()));
}
```

*Implementation Analysis:* the formatting logic left for `NameFormatter` (Item 5), the useless connection call and the never-run branch were deleted (Items 8 and 6), and the raw `List` with its cast became `List<Employee>`. What remains is two tiny methods, each with one job. For the same list of names, the old and new versions produce identical `EMP:` lines.

**Smell 2 — Large Class (God Class)**

*Definition:* a class that has too many responsibilities. *Why bad:* it violates SRP (Section 3.4, Single Responsibility Principle) and is hard to maintain, test or reuse. *Standard fix:* Extract Class (move each group of related fields and methods into its own class).

Bad example (method bodies elided):

```java
class OrderManager {
    void createOrder(Order o) { /* ... */ }
    void cancelOrder(Order o) { /* ... */ }
    void printInvoice(Order o) { /* ... */ }
    void sendEmail(Order o) { /* ... */ }
    void updateInventory(Order o) { /* ... */ }
    void processRefund(Order o) { /* ... */ }
    void generateSalesReport() { /* ... */ }
}
```

Better version (responsibilities split into cohesive, independent classes):

```java
class OrderService { void createOrder(Order o) { /* ... */ } }
class CancellationService { void cancelOrder(Order o) { /* ... */ } }
class InvoicePrinter { void printInvoice(Order o) { /* ... */ } }
class EmailService { void sendEmail(Order o) { /* ... */ } }
class InventoryService { void updateInventory(Order o) { /* ... */ } }
class RefundService { void processRefund(Order o) { /* ... */ } }
class SalesReportGenerator { void generateReport() { /* ... */ } }
```

*Implementation Analysis:* the seven methods cover order handling, documents, messaging, stock, money and reporting. After the split, each class has one purpose and one reason to change. The `/* ... */` bodies mean the example shows structure only.

**Lab Sheet 3, Part C, Item 2 (Large Class).** `EmployeeService` in the provided project is the God Class: a five-method interface (storage, payslips, CSV export, promotion e-mail, org chart) plus name formatting plus a processing loop. The fix, covered in Section 3.4 (Single Responsibility Principle), Task 11, and Section 3.7 (Interface Segregation Principle), Task 14, was to split the interface into five small ones, move formatting into `NameFormatter`, and let `EmployeeService` implement only `EmployeeStorage`, `EmployeeExporter` and `PromotionNotifier`. `Department` was also thinned (Items 4 and 7).

**Smell 3 — Primitive Obsession and Data Clumps**

*Definitions:* **Primitive Obsession** is overusing primitive types (strings, numbers, booleans) instead of small domain-specific objects. **Data Clumps** are groups of variables that are often passed around together. *Why bad:* the fields or properties appear together everywhere, so logic gets duplicated. *Standard fixes:* Extract Class (make the clump a class); Replace Data Value with Object; Replace Type Code with Class.

Bad example:

```java
class Customer {
    String firstName;
    String lastName;
    String street;
    String city;
    String postalCode;
}
```

Better version:

```java
class Address { String street; String city; String postalCode; }
class Customer { String firstName; String lastName; Address address; }
```

*Implementation Analysis:* `street`, `city` and `postalCode` always travel together, so they form a clump. Grouping them into `Address` gives that concept one home; anything that formats, validates or compares addresses now goes in `Address`, and other classes reuse it. Encapsulation reduces duplication and improves clarity.

**Lab Sheet 3, Part C, Item 3 (Primitive Obsession / Data Clumps).** The provided project has three instances:

1. `Employee` stored `city` and `zip` as two loose strings (commented "data clump"). Fix: an `Address` record.
2. `Department` used an `int code` where 1, 2 and 3 secretly meant head office, regional and field. Fix: a `DepartmentLevel` enum (see Item 4).
3. `Employee` used three boolean flags (`isActive`, `isOnLeave`, `isTemp`) that allowed nonsense combinations such as "inactive and on leave". Fix: an `EmploymentStatus` enum and a `isTemporary()` method that the contractor subclass overrides.

```java
// BEFORE (Employee)
public String city, zip;  // data clump
public boolean isActive = true, isOnLeave = false, isTemp = false; // too many flags (smell)

// AFTER
public record Address(String city, String zip) { }

public enum EmploymentStatus { ACTIVE, ON_LEAVE, INACTIVE }

// in Employee:
private Address address;
private EmploymentStatus status = EmploymentStatus.ACTIVE;
public boolean isTemporary() { return false; }   // ContractEmployee overrides to return true
```

*Implementation Analysis:* a Java `record` is a short way to declare an immutable data holder; the compiler generates the constructor, accessors, `equals`, `hashCode` and `toString`. An enum limits a value to a fixed set of named choices, so an impossible state cannot be written.

**Smell 4 — Long Parameter List**

*Definition:* too many parameters in a method. *Why bad:* hard to read, easy to put arguments in the wrong order, and difficult to extend. *Standard fixes:* Introduce Parameter Object; Preserve Whole Object (pass the object instead of several values taken from it).

Bad example:

```java
void createOrder(String customerName, String street, String city,
                 String postalCode, List<Item> items, double discount) { /* ... */ }
```

Better version:

```java
void createOrder(Customer customer, List<Item> items,
                 DiscountPolicy discount) { /* ... */ }
```

*Implementation Analysis:* four of the six parameters (name, street, city, postal code) describe one customer, and `discount` is passed as a bare `double`. Passing objects instead of many related primitives shortens the signature and stops accidental mix-ups such as swapping `street` and `city` (both are `String`, so the compiler cannot detect it).

**Possible Exam Angles (with model answers): smells 1–4**

**Q1:** Name the smell and the standard fix.
```java
void register(String first, String last, String street, String city,
              String zip, String phone, boolean sendMail) { /* ... */ }
```

**A1:** Long Parameter List (with Data Clumps in `street`, `city`, `zip`). Fix: Introduce Parameter Object: group the related values into objects such as `Address` and `Customer` and pass those.

**Q2:** A 300-line method has comments like `// validate input`, `// compute totals`, `// write file`. Name the smell, the standard fix and where the comments point.

**A2:** Long Method. Fix: Extract Method. The comments mark natural boundaries: each commented block becomes its own method named after what the comment says (`validateInput`, `computeTotals`, `writeFile`), and the comments become unnecessary.

**Q3:** `class Invoice { String currencyCode; double amount; String customerStreet; String customerCity; ... }` appears in five classes with the same field groups. Identify the smells and a fix.

**A3:** Data Clumps (the same group of fields repeated in several classes) and Primitive Obsession (a currency and an amount held as a `String` and a `double` instead of an object). Fix: extract small classes such as `Money` and `Address` and use them wherever the groups appeared.

**Q4:** Which SOLID principle does a Large Class (God Class) most directly violate, and why?

**A4:** The Single Responsibility Principle, because such a class has many unrelated reasons to change.

---

**Smell 5 — Switch/If-Else Chains**

*Definition:* using long conditional chains to determine behaviour. *Why bad:* it violates the Open/Closed Principle (Section 3.5, Open/Closed Principle), and adding a new customer type requires editing the same code. *Standard fix:* Replace Conditional with Polymorphism (each branch becomes a class implementing a common interface). The full route on refactoring.guru: isolate the switch with Extract Method and Move Method; if it switches on a type code, use Replace Type Code with Subclasses or Replace Type Code with State/Strategy; then Replace Conditional with Polymorphism. It also says to leave a switch alone when it performs simple actions, or when a factory uses it to choose which class to create.

Bad example:

```java
double getDiscount(Order o) {
    if (o.customerType.equals("REGULAR")) return 0.05;
    else if (o.customerType.equals("PREMIUM")) return 0.10;
    else if (o.customerType.equals("STUDENT")) return 0.15;
}
```

> ⚠️ **Flag (the bad example is not compilable as printed):** Java requires a `return` on every path, and the last `else if` has no final `else` or `return`, so this method as printed would not compile ("missing return statement"). The slide is showing the shape of the smell; if you copy it, add a final `return 0.0;`.

Better version:

```java
interface DiscountPolicy { double getDiscount(Order o); }
class RegularDiscount implements DiscountPolicy { double getDiscount(Order o) { return 0.05; } }
class PremiumDiscount implements DiscountPolicy { double getDiscount(Order o) { return 0.10; } }
class StudentDiscount implements DiscountPolicy { double getDiscount(Order o) { return 0.15; } }
```

*Implementation Analysis:* the chain's decision (which discount applies) is replaced by object type: the caller holds a `DiscountPolicy` and calls `getDiscount`, and the right rate comes from whichever implementation it was given. It is easy to extend without modifying existing classes.

> ⚠️ **Flag (compile detail in the good example):** Methods that implement an interface method must be declared `public`. The slide's good example leaves `public` out for brevity; in real code write `public double getDiscount(Order o)`.

**Lab Sheet 3, Part C, Item 4 (Switch / If-Else Chains).** `Department` in the provided project had *two* chains doing the same numeric-code-to-text job, plus magic numbers:

```java
// BEFORE
public int code;   // 1=HQ, 2=REGIONAL, 3=FIELD (magic numbers)

public String levelName() {
    switch (code) {
        case 1: return "Headquarters";
        case 2: return "Regional";
        case 3: return "Field";
        default: return "Unknown";
    }
}

public String getLevelVerbose() {
    if (code == 1) return "Headquarters (HQ)";
    else if (code == 2) return "Regional (R)";
    else if (code == 3) return "Field (F)";
    else return "Unknown (?)";
}
```

```java
// AFTER: the enum carries its own display data, so no conditional remains
public enum DepartmentLevel {
    HEADQUARTERS("Headquarters", "Headquarters (HQ)"),
    REGIONAL("Regional", "Regional (R)"),
    FIELD("Field", "Field (F)");

    private final String displayName;
    private final String verboseName;

    DepartmentLevel(String displayName, String verboseName) {
        this.displayName = displayName;
        this.verboseName = verboseName;
    }

    public String getDisplayName() { return displayName; }
    public String getVerboseName() { return verboseName; }
}
```

*Implementation Analysis:* each constant is an object holding both of its names, so the two chains disappear and so does the duplicated mapping between them. Adding a level is one new constant; no method has to change. (Strictly, this is Replace Type Code with Class rather than full polymorphism; the fully polymorphic alternative would give each level its own class. For a pure lookup of names, the enum is simpler and gives the same OCP benefit.)

**Smell 6 — Shotgun Surgery**

*Definition:* one change requires many edits in multiple classes. *Bad example:* adding a new tax rule requires updating `OrderService`, `InvoicePrinter`, `PaymentService` and `ReportGenerator`. *Why bad:* there is a risk of missing updates, and it increases development time and bugs. *Standard fixes:* Move Method and Move Field (gather the scattered logic into one class).

Better version:

```java
class TaxCalculator { double calculateTax(Order o) { /* ... */ } }
```

*Implementation Analysis:* the tax rule was copied or spread across four classes. Centralising it in `TaxCalculator` means the four classes call one method, so a rule change is made once.

**Smell 7 — Divergent Change**

*Definition:* a class changes for multiple, unrelated reasons. *Why bad:* payment, invoice and marketing logic are unrelated. *Standard fix:* Extract Class.

Bad example:

```java
class OrderProcessor {
    void processPayment(Order o) { /* ... */ }
    void generateInvoice(Order o) { /* ... */ }
    void sendPromotionalEmail(Customer c) { /* ... */ }
}
```

Better version (classes have one reason to change):

```java
class PaymentProcessor { void processPayment(Order o) { /* ... */ } }
class InvoiceService { void generateInvoice(Order o) { /* ... */ } }
class MarketingService { void sendPromotionalEmail(Customer c) { /* ... */ } }
```

*Implementation Analysis:* a change to payment rules, invoice layout or marketing wording would each edit `OrderProcessor`, so it changes for three unrelated reasons. Note the difference from Shotgun Surgery, which is the opposite direction: **Divergent Change is one class edited for many unrelated reasons; Shotgun Surgery is one change requiring edits in many classes.** Divergent Change is the SRP violation seen from the change-history side.

**Smell 8 — Duplicate Code**

*Definition:* the same logic appears in multiple places. *Why bad:* bug fixes and changes require changes in multiple places. *Standard fixes* (refactoring.guru): the same code in two methods of one class: Extract Method. The same code in two sibling subclasses: Extract Method in both, then Pull Up Field for the fields it uses (Pull Up Constructor Body if the duplicate is inside a constructor). Similar but not identical code: Form Template Method. Two methods doing the same thing with different algorithms: Substitute Algorithm. The same code in two unrelated classes: Extract Superclass, or, if that is difficult, Extract Class in one class and use the new component in the other.

> ⚠️ **Flag (technique names differ between sources):** For sibling subclasses, Martin Fowler's book says Extract Method followed by *Pull Up Method*, while refactoring.guru (the mandatory reference) says Extract Method followed by *Pull Up Field*. Both describe moving the shared code up into the superclass. If a quiz option lists only one of them, pick it; if both are offered, prefer refactoring.guru's wording.

Bad example:

```java
// in OrderService
double calculateOrderTotal(Order o) { /* ... */ }

// in ReportGenerator
double calculateOrderTotal(Order o) { /* ... */ }
```

Better version:

```java
class OrderCalculator { double calculateTotal(Order o) { /* ... */ } }
```

*Implementation Analysis:* the two identical methods become one method in one class that both callers use. Centralised logic improves maintainability: fix a bug once and it is fixed everywhere.

**Lab Sheet 3, Part C, Item 5 (Duplicate Code).** The provided project contained the same "trim, then capitalise the first letter and lower-case the rest" logic seven times:

| Where (original) | Method |
|---|---|
| `Utility` | `formatName` |
| `HashUtil` | `normalizeName` |
| `Repository` | `trimCap` |
| `CsvEmployeeRepository` | `trimAndCap` (private, unused) |
| `DbEmployeeRepository` | `trimAndCap` (private, unused) |
| `EmployeeService` | `formatEmployeeName1` |
| `EmployeeService` | inline inside `processEverything` |

`EmployeeService.formatEmployeeName2` was an eighth method that only called `formatEmployeeName1`, adding a second name for the same thing. (The "duplicated 1/2/3 level mapping" in `Department` is covered in Item 4.)

```java
// AFTER: one home for the rule
public final class NameFormatter {
    private NameFormatter() {
        throw new AssertionError("NameFormatter is a utility class and must not be instantiated");
    }

    public static String format(String name) {
        if (name == null) { return ""; }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) { return ""; }
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }
}
```

*Implementation Analysis:* the `null` and empty checks return `""` (matching all the old copies). `Locale.ROOT` makes the case conversion independent of the machine's language settings. The class is `final` with a private constructor that throws, so it can only be used through its static method. The new method was tested against the old public copies on inputs including `null`, blank, mixed-case and accented text, with identical results.

**Smell 9 — Dead Code**

*Definition:* code that is never used but still exists in the code base. *Why bad:* it causes confusion and wastes mental effort during maintenance; a real-world example is the Knight Capital failure (Section 3.1, Design vs. Implementation, and Why Implementations Fail). *Standard fix:* delete the unused code and any unneeded files, and keep the history in version control if it may be needed again. (Before deleting, check that nothing uses it indirectly, for example through reflection or as a public interface.) Two related treatments (checked against refactoring.guru's own page for this smell): for an unnecessary *class*, Inline Class (merge its features into another class) or Collapse Hierarchy (merge a subclass into its superclass, or the reverse, when they are no longer different) can be applied; and for unneeded *parameters*, Remove Parameter.

Typical forms are an `if (false)` block, a method nobody calls, an unused variable, parameter or field, and a class nothing refers to. Dead code can also hide inside a complex conditional, as a branch that can never be reached (because of an error or because circumstances changed). It usually appears because requirements changed or corrections were made and nobody had time to clean up the old code. The quickest way to find it is to use a good IDE, which highlights unused code and unreachable branches.

*Implementation Analysis:* the danger is not the wasted space but that a reader must work out whether the code matters, and that dormant code can be revived by accident, as it was at Knight Capital.

**Lab Sheet 3, Part C, Item 6 (Dead Code).** Dead code found in the provided project and what was done:

| Dead code | Action |
|---|---|
| `if (false) { System.out.println("Never runs"); }` in `processEverything` | Deleted |
| `Utility.dumpToConsole` (never called) | Deleted |
| `Employee.clone()` and `Cloneable` (never used; shallow copy with a console print) | Deleted |
| `ContractEmployee.tostrng()` (always throws) and its unused `Random` field | Deleted |
| `HashUtil`, `CsvEmployeeRepository`, `DbEmployeeRepository` (never referenced) | Deleted |
| `DataSource` (used only by the old `Repository` and the removed message chain, Item 8) | Deleted once nothing used it |

**Smell 10 — Temporary Field**

*Definition:* a field only used under certain conditions. *Why bad:* it misleads readers into thinking the field is always relevant, and makes code harder to read.

Bad example:

```java
class Order { double seasonalDiscount; /* only used for holiday sales */ }
```

Better version (the field exists only where needed):

```java
class SeasonalOrder extends Order { double seasonalDiscount; }
```

*Implementation Analysis:* for most orders `seasonalDiscount` sits unused, which raises the question "is it always meaningful?". Moving it to a subclass used only for holiday sales confines it to the case where it applies.

> ⚠️ **Flag (the slide's fix is one of several):** The slide's fix moves the field into a subclass. refactoring.guru's recommended treatments for Temporary Field are Extract Class (put the temporary fields and the code that uses them into a separate class, effectively creating a method object) and Introduce Null Object (replacing the conditional checks on the field). All are ways to stop an always-present field from being sometimes-meaningless. Use the slide's subclass version if asked to match the lecture.

**Smell 11 — Middle Man**

*Definition:* a class that only delegates work to another class. *Why bad:* it adds unnecessary indirection without extra value. *Standard fix:* Remove Middle Man: remove the middle man and call the class instances directly. refactoring.guru notes that this smell can result from overzealous elimination of Message Chains (smell 13), and that a middle man should be kept if it was created for a reason: to avoid dependencies between classes, or because a design pattern such as Proxy or Decorator deliberately uses one.

Bad example:

```java
class OrderManager {
    private OrderService orderService;
    void createOrder(Order o) { orderService.createOrder(o); }
}
```

*Implementation Analysis:* `OrderManager.createOrder` does nothing except forward to `OrderService.createOrder`. Callers could use `OrderService` directly, so the extra class only adds a hop to read through.

**Possible Exam Angles (with model answers): smells 5–11**

**Q1:** Identify the smell in `if (type == 1) {...} else if (type == 2) {...} else if (type == 3) {...}` appearing in three different methods of one class, and give the fix.

**A1:** Switch/If-Else Chains, repeated in several places, which also breaks the Open/Closed Principle because a new type means editing every chain. Fix: Replace Conditional with Polymorphism: an interface with one implementation per type (or, for a simple lookup of names, an enum that carries the data).

**Q2:** Adding a new currency requires edits in `Cart`, `Invoice`, `Refund` and `Report`. Which smell is this, and what is the fix?

**A2:** Shotgun Surgery: one conceptual change forces many edits across classes. Fix: gather the scattered currency logic into one class (Move Method and Move Field), for example a `CurrencyConverter`, so a change is made in one place.

**Q3:** One class `StudentManager` is edited when grading rules change, when the transcript layout changes and when the e-mail templates change. Which smell, and how does it differ from Shotgun Surgery?

**A3:** Divergent Change: one class is changed for several unrelated reasons. It differs from Shotgun Surgery in direction: Divergent Change is one class with many reasons to change; Shotgun Surgery is one reason to change spread over many classes. Fix: Extract Class, splitting it into grading, transcript and e-mail classes.

**Q4:** Two classes each contain an identical 12-line method that validates an e-mail address. Name the smell and the fix.

**A4:** Duplicate Code. Fix: put the method in one shared place and have both classes call it (Extract Class, for example an `EmailValidator` class, or Extract Superclass if the two classes are closely related), so a bug fix or rule change is made once.

**Q5:** A class `Report` has a field `taxExemptionCertificate` that is `null` for 99% of reports and used only when generating an export for one specific customer type. Name the smell and one fix.

**A5:** Temporary Field. One fix is to move the field into a subclass used only for the special case; alternatively, Extract Class for the special-case data and code.

**Q6:** `class OrderFacade { private OrderService s; void create(Order o){ s.create(o);} void cancel(Order o){ s.cancel(o);} }` adds no logic. Name the smell and the fix.

**A6:** Middle Man: the class only forwards calls. Fix: Remove Middle Man: callers use `OrderService` directly and `OrderFacade` is removed.

---

**Smell 12 — Feature Envy**

*Definition:* a method that uses more data from another class than from its own. *Why bad:* it knows too much about the other class's internals, resulting in very tight coupling. *Standard fix:* Move Method (move the behaviour to the class where the data lives), after an Extract Method first if only part of the method is envious.

*Further points* (checked against refactoring.guru's own page for this smell):

- *Where it comes from:* the smell often appears after fields are moved into a data class (a class that mostly holds data), leaving the operations on that data behind in other classes. If so, move the operations to the data class as well.
- *The guiding rule:* things that change at the same time should be kept in the same place, and data and the functions that use it usually change together.
- *A method that uses several classes:* work out which class holds most of the data it uses and place the method there, along with that data; or use Extract Method to split it into parts that can each go to a different class.
- *When to leave it alone:* sometimes behaviour is deliberately kept separate from the class that holds the data, so that the behaviour can be changed dynamically (the Strategy and Visitor design patterns do this on purpose). Not every method that reads another class's data is a smell.

Bad example:

```java
class DiscountService {
    double calculateDiscount(Order o) {
        return o.getItems().size() * o.getCustomer().getLoyaltyPoints();
    }
}
```

Better version:

```java
class Order { double calculateDiscount() { /* ... */ } }
```

*Implementation Analysis:* every value in the calculation is fetched from the `Order` (its items and its customer's points), and none from `DiscountService` itself. The behaviour belongs with the data, so it moves into `Order`, and `Order` can change how it stores items or customers without breaking an outside class.

**Lab Sheet 3, Part C, Item 7 (Feature Envy).** In the provided project, `Department` built a sentence that is really about an employee:

```java
// BEFORE (in Department)
public String describeEmployee(Employee e) {
    return "Employee: " + e.name + " works in " + this.name;
}
```

```java
// AFTER (in Employee): the method that talks about the employee lives with the employee
public String describeWorkplace(Department department) {
    return "Employee: " + name + " works in " + department.getName();
}
```

*Implementation Analysis:* the original reached into the employee's public field (`e.name`) from another class. The moved method uses the employee's own private `name` and asks the department only for its name through a getter. The output text is identical.

**Smell 13 — Message Chains (Law of Demeter Violation)**

*Definition:* chaining calls to navigate through multiple objects. *Why bad:* it breaks encapsulation, and changes in the data structure ripple through all callers. *Standard fix:* Hide Delegate: add a method that does the navigation inside, so callers ask for what they need directly.

Bad example:

```java
order.getCustomer().getAddress().getCity();
```

Better version:

```java
order.getCustomerCity();
```

*Implementation Analysis:* the caller must know that an order has a customer, that a customer has an address, and that an address has a city. If any link in the chain changes, every such caller must change. Letting each object expose only what is needed hides the path.

> ⚠️ **Flag (the Law of Demeter is only named on the slide):** The slide names the Law of Demeter without defining it. In short, it says an object should talk only to its immediate collaborators and not reach through them to distant objects ("only talk to your friends"). Message Chains is the code-level symptom of breaking it. One caution from refactoring.guru: if you create too many delegating methods, the class can become a Middle Man (smell 11).

**Lab Sheet 3, Part C, Item 8 (Message Chains).** The provided `EmployeeService` contained:

```java
// BEFORE
if (getRepository() != null) {
    getRepository().getDataSource().getConnection().toString();
}
```

*Implementation Analysis:* the chain runs through three objects (repository, data source, connection), and its result is thrown away, so it did nothing useful. The refactored service no longer knows about connections at all: the chain and the `getRepository()` accessor were removed, and the service uses only an `EmployeeRepository` abstraction (Section 3.8, Dependency Inversion Principle). If the information were needed, Hide Delegate (for example a method `repository.describeConnection()`) would be the standard fix.

**Other defects fixed in the provided project (not on the lab list).** The project also contained getters with side effects (`Employee.getSalary` added 1.0 each time it was read; `Department.getCode` incremented), public mutable fields, a static mutable counter, `equals` and `hashCode` that disagreed, `compareTo` that compared name lengths, a pointless `synchronized`, and a `toString` that printed to the console. All were removed or corrected. These are further examples of poor implementation, but they are not among the 13 named smells.

> ⚠️ **Flag (the refactored project is not behaviour-identical in a few deliberate ways):** Output now goes through a logger instead of `System.out` (Part A, answered in Week 4's guide (Best Practices and Refactoring Techniques)); the year and month values changed from `126` and `8` to `2026` and `9` because the deprecated date methods were replaced; `getSalary()` no longer increases the salary on each read; `Employee` no longer accepts a `null` name; and the root package was renamed from `com.sliit.badcode` to `com.sliit.employeesystem`. The employee lines printed for the demo (`Employee{E001, Alice}`) are unchanged.

**Possible Exam Angles (with model answers): smells 12–13**

**Q1:** Name the smell and fix it.
```java
class Payroll {
    double bonus(Employee e) {
        return e.getYearsOfService() * e.getPerformanceScore() * e.getBaseSalary();
    }
}
```

**A1:** Feature Envy: `bonus` uses only `Employee` data and nothing from `Payroll`. Fix: Move Method: put `calculateBonus()` in `Employee`, where the data lives.

**Q2:** Name the smell and the principle it violates: `String city = invoice.getOrder().getCustomer().getAddress().getCity();`

**A2:** Message Chains, which violates the Law of Demeter: the caller knows the structure of three intermediate objects. Fix: Hide Delegate: give `Invoice` a method such as `getCustomerCity()` that performs the navigation internally.

**Q3:** Why can fixing Message Chains with many delegating methods go wrong?

**A3:** Each hidden step needs a forwarding method on the outer class. Too many of them turn the class into a pure pass-through, which is the Middle Man smell. The fix is a balance: hide the parts that callers should not know about, and do not add forwarding for everything.

**Q4:** How is Feature Envy related to Message Chains in `o.getCustomer().getLoyaltyPoints()`?

**A4:** Both are coupling smells appearing together. The method uses another object's data (Feature Envy), and reaches it by navigating through an intermediate object (Message Chain). Moving the behaviour to the class that owns the data removes both.

**Q5:** A `SortStrategy` class has a method `sort(List<Item> items)` that only reads the items it is given. A reviewer says this is Feature Envy and it should be moved into `List`. Is the reviewer right?

**A5:** Not necessarily. A method that uses another class's data is only a smell when that behaviour naturally belongs with the data. Here the behaviour is deliberately kept separate so that different sorting behaviours can be swapped in (the Strategy pattern), which is a recognised reason to leave it alone. Moving `sort` into `List` would remove that flexibility.

---

### 3.12 Code Smells Compared and Mapped to Fixes

**Every smell with its group, its telltale sign, the SOLID principle behind it, and the standard fix.** The fixes use refactoring.guru's technique names (see the flag in Section 3.11, The Common Code Smells).

| Smell | Group | Telltale sign in a snippet | SOLID link | Standard fix |
|---|---|---|---|---|
| Long Method | Bloaters | One method doing many steps; comments marking sections | SRP | Extract Method |
| Large Class (God Class) | Bloaters | One class with many unrelated methods | SRP | Extract Class |
| Primitive Obsession | Bloaters | Strings, ints or booleans standing in for concepts (a code `1/2/3`, flags) | none | Replace Data Value with Object; Replace Type Code with Class |
| Data Clumps | Bloaters | The same group of fields or parameters appearing together | none | Extract Class; Introduce Parameter Object |
| Long Parameter List | Bloaters | More than three or four parameters | none | Introduce Parameter Object; Preserve Whole Object |
| Switch/If-Else Chains | Object-Orientation Abusers | if/else or switch choosing behaviour by type | OCP | Replace Conditional with Polymorphism |
| Temporary Field | Object-Orientation Abusers | A field that is empty most of the time | none | Extract Class (or move to a subclass); Introduce Null Object |
| Alternative Classes with Different Interfaces | Object-Orientation Abusers | Two classes, same job, different method names | none | Rename Method / Move Method; Extract Superclass |
| Divergent Change | Change Preventers | One class edited for unrelated reasons | SRP | Extract Class |
| Shotgun Surgery | Change Preventers | One change forces edits in many classes | none | Move Method / Move Field |
| Duplicate Code | Dispensables | The same logic in two or more places | none | Extract Method (same class); Extract Method + Pull Up Field (sibling subclasses); Extract Superclass or Extract Class (unrelated classes) |
| Dead Code | Dispensables | Code never called or reached (`if (false)`, an unreachable branch, an unused parameter) | none | Delete it; Inline Class or Collapse Hierarchy for an unneeded class; Remove Parameter for an unused parameter |
| Feature Envy | Couplers | A method uses mostly another class's data | none | Move Method; Extract Method first if only part is envious. Leave it alone when the separation is deliberate (Strategy, Visitor) |
| Inappropriate Intimacy | Couplers | A class uses another's internal fields and methods | none | Move Method / Move Field; Hide Delegate |
| Message Chains | Couplers | `a.getB().getC().getD()` | none (Law of Demeter) | Hide Delegate |
| Middle Man | Couplers | A class that only forwards calls | none | Remove Middle Man |

**Look-alikes that quizzes like to separate:**

| Pair | How to tell them apart |
|---|---|
| Shotgun Surgery vs Divergent Change | Shotgun Surgery: one change, many classes. Divergent Change: one class, many kinds of change. |
| Large Class vs Divergent Change | Large Class is judged by size and mixed responsibilities in the code; Divergent Change is judged by *why the class keeps being edited*. A God Class typically shows both. |
| Long Parameter List vs Data Clumps vs Primitive Obsession | Long Parameter List is about the number of parameters. Data Clumps is about the same values recurring together (in fields or parameters). Primitive Obsession is about using primitives where a small object should exist. One signature can show all three. |
| Message Chains vs Middle Man | Message Chains: the caller navigates too much. Middle Man: a class delegates too much. The standard fix for the first (Hide Delegate) can, if overdone, create the second. |
| Feature Envy vs Inappropriate Intimacy | Feature Envy: a *method* wants another class's data. Inappropriate Intimacy: two *classes* know too much about each other's internals. |
| Duplicate Code vs Dead Code | Duplicate Code is used many times; Dead Code is used never. Both are dispensable, and both are fixed by consolidating or deleting. |
| Switch/If-Else Chains vs OCP | The smell is what you see in the code; OCP is the principle it breaks. |

**Possible Exam Angles (with model answers)**

**Q1:** Choose the best refactoring technique for each smell: (a) Long Method, (b) Large Class, (c) Long Parameter List, (d) Switch/If-Else Chains, (e) Feature Envy, (f) Message Chains, (g) Middle Man, (h) Dead Code.

**A1:** (a) Extract Method; (b) Extract Class; (c) Introduce Parameter Object (or Preserve Whole Object); (d) Replace Conditional with Polymorphism; (e) Move Method; (f) Hide Delegate; (g) Remove Middle Man; (h) delete the unused code (history stays in version control); for an unneeded class, Inline Class or Collapse Hierarchy; for an unused parameter, Remove Parameter.

**Q2:** Read the snippet and identify the smells and SOLID violations, then state one fix each.
```java
class OrderHandler {
    private SmsService sms = new SmsService();

    double price(String tier, int qty, double base, String street, String city, String zip) {
        if (tier.equals("GOLD")) return qty * base * 0.8;
        else if (tier.equals("SILVER")) return qty * base * 0.9;
        return qty * base;
    }
    void save(Order o) { /* ... */ }
    void print(Order o) { /* ... */ }
}
```

**A2:** Smells: Long Parameter List (six parameters), Data Clumps (`street`, `city`, `zip`), Switch/If-Else Chain (the `tier` chain), Large Class (pricing, saving and printing together), and magic numbers (`0.8`, `0.9`). SOLID: OCP (a new tier needs editing `price`), SRP (pricing, persistence and printing in one class), DIP (`new SmsService()` inside the class). Fixes: Introduce Parameter Object for the address; Replace Conditional with Polymorphism using a `PricingPolicy` interface; Extract Class for saving and printing; inject an interface for the notification service through the constructor.

**Q3:** A developer removes `order.getCustomer().getAddress().getCity()` by adding forwarding methods for every property of `Customer`, `Address` and `Payment` on `Order`. What new smell may appear, and why?

**A3:** Middle Man. `Order` would then contain many methods that do nothing except delegate, adding indirection without extra value. Hide only what callers should not need to know, rather than mirroring every property.

**Q4:** True or false, with a reason: "Fixing a code smell always changes the program's behaviour."

**A4:** False. The purpose of removing a smell through refactoring is to improve the internal structure of the code (making it easier to understand and cheaper to change) without changing its observable behaviour. Smells are symptoms, not bugs, so fixing them should leave the program's results the same.

---

**Further reading listed for this topic:** Tom Dalling's articles on SOLID class design (Category: Software Design); the *Code Smells* catalogue on refactoring.guru (mandatory); and *Case Study 4: The $440 Million Software Error at Knight Capital* (a detailed case study of the Knight Capital failure, see Section 3.1, Design vs. Implementation, and Why Implementations Fail).

**Chapter Summary**

Good design can fail if it is poorly implemented. The SOLID principles guide better coding decisions: one reason to change per class (SRP); add features by extending instead of editing (OCP); subclasses that work anywhere their parent does (LSP); small, client-specific interfaces (ISP); and dependence on abstractions instead of concrete classes (DIP). Code smells are indicators of deeper design or implementation issues, not bugs. Detecting and fixing smells early reduces technical debt, and refactoring is a continuous process, taken up in Week 4's guide (Best Practices and Refactoring Techniques).

---

**Flags to Review**

Every flag marked in this chapter, in the order it appears. Items 1–4 need only a glance; the rest matter when a detail is tested.

1. **Scope and exclusions** (start of chapter): administrative slides and decorative images were left out; the reference list was kept.
2. **No past papers** (start of chapter): exam-style questions are calibrated from the quiz brief and the lab sheet only.
3. **Numbering assumptions** (start of chapter): "Lecture 3" came from you, and "Week 3" is assumed; check the chapter number and filename.
4. **Lab sheet split across two chapters** (start of chapter): Part B and Part C are answered here; Part A is answered in Week 4's guide (Best Practices and Refactoring Techniques).
5. **Knight Capital details are a supplement** (Section 3.1, Design vs. Implementation, and Why Implementations Fail): checked against Knight's own filings and the SEC's 2013 order; the SEC gives about 45 minutes and more than $460 million, the slide gives 30 minutes and $440 million, and the SEC order supports "lack of quality control" more directly than "time pressure".
6. **Added prerequisite section** (Section 3.2, Foundations Used Throughout): definitions of cohesion, coupling, abstraction, polymorphism, dependency injection and test doubles were added because they are used but never defined.
7. **Two different `DiscountPolicy` shapes** (Section 3.5, Open/Closed Principle, and Section 3.11, The Common Code Smells): one has `getDiscount()`, the other `getDiscount(Order o)`; they are separate examples.
8. **"LSP-preserving" labels on `SeasonalOrder` and `BulkOrder`** (Section 3.6, Liskov Substitution Principle): follow the labels for the quiz, but read strictly they also change the requested discount.
9. **Grouping of smells is a supplement** (Section 3.10, Code Smells: What They Are): the five groups come from refactoring.guru, not from the slides.
10. **Two smells appear only in the figure** (Section 3.10, Code Smells: What They Are): Alternative Classes with Different Interfaces and Inappropriate Intimacy are defined here, and both definitions and fixes were checked against refactoring.guru's own pages.
11. **Two separately numbered lists in the lab sheet** (Section 3.11, The Common Code Smells): Part C restarts at 1, so its answers are labelled "Item N"; the lab covers only eight of the 13 list entries.
12. **How the standard fix is named** (Section 3.11, The Common Code Smells): technique names are refactoring.guru's, not the slides'; Week 4's guide (Best Practices and Refactoring Techniques) explains the techniques.
13. **Long Method: the slide's two versions differ** (Section 3.11, The Common Code Smells, Smell 1): the "better" version renames one call and omits another.
14. **Switch chain bad example does not compile as printed** (Section 3.11, The Common Code Smells, Smell 5): it lacks a final `return`.
15. **Switch chain good example omits `public`** (Section 3.11, The Common Code Smells, Smell 5): interface implementations must be `public`.
16. **Technique names differ between sources** (Section 3.11, The Common Code Smells, Smell 8): Fowler's book says Pull Up Method where refactoring.guru says Pull Up Field.
17. **Temporary Field: the slide's fix is one of several** (Section 3.11, The Common Code Smells, Smell 10): Extract Class and Introduce Null Object are also standard treatments.
18. **Law of Demeter is only named on the slide** (Section 3.11, The Common Code Smells, Smell 13): a short definition was added.
19. **The refactored project is not behaviour-identical in a few deliberate ways** (Section 3.11, The Common Code Smells, after Smell 13): logging output, year and month values, `getSalary()`, null names and the package name changed.
