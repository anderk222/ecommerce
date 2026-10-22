# Arquitectura hexagonal con Spring Boot — guía práctica

**Acompaña a:** [Hexagonal Architecture with Spring Boot, Part 2](https://vitelco.com/vitelco-blog/hexagonal-architecture-with-spring-boot-part-2) (Vitelco)

La Parte 2 explica el vocabulario y dibuja el árbol de paquetes, pero deliberadamente no escribe código. Eso deja sin respuesta las preguntas más importantes: cómo se ve un puerto de verdad, quién llama a quién, dónde va `@Transactional`, qué se mapea con qué y cómo evitar que la arquitectura se degrade en seis meses.

Esta guía responde esas preguntas con un segmento completo y ejecutable: **cancelar una suscripción**, de principio a fin. Cada archivo se muestra completo, en el orden en que lo crearías.

---

## Índice

1. [El modelo mental en una página](#1-el-modelo-mental-en-una-página)
2. [El vocabulario, con precisión](#2-el-vocabulario-con-precisión)
3. [La regla de dependencia (la única regla)](#3-la-regla-de-dependencia-la-única-regla)
4. [Estructura de paquetes: dos formas válidas](#4-estructura-de-paquetes-dos-formas-válidas)
5. [El ejemplo, de principio a fin](#5-el-ejemplo-de-principio-a-fin)
   - [5.1 El segmento que vamos a construir](#51-el-segmento-que-vamos-a-construir)
   - [5.2 Dominio: Java puro, sin Spring](#52-dominio-java-puro-sin-spring)
   - [5.3 Puertos de salida](#53-puertos-de-salida)
   - [5.4 Puertos de entrada (casos de uso)](#54-puertos-de-entrada-casos-de-uso)
   - [5.5 Servicios de aplicación](#55-servicios-de-aplicación)
   - [5.6 El adaptador web](#56-el-adaptador-web)
   - [5.7 El adaptador de persistencia](#57-el-adaptador-de-persistencia)
   - [5.8 Adaptadores externos: pago y correo](#58-adaptadores-externos-pago-y-correo)
   - [5.9 Cablear el hexágono](#59-cablear-el-hexágono)
   - [5.10 Probar el segmento](#510-probar-el-segmento)
6. [Las preguntas que el artículo deja abiertas](#6-las-preguntas-que-el-artículo-deja-abiertas)
7. [Migrar una aplicación CRUD existente](#7-migrar-una-aplicación-crud-existente)
8. [Señales de alarma y antipatrones](#8-señales-de-alarma-y-antipatrones)
9. [Chuleta](#9-chuleta)
10. [Lecturas recomendadas](#10-lecturas-recomendadas)

---

## 1. El modelo mental en una página

Un hexágono tiene un **dentro** y un **fuera**. Esa es toda la idea.

- **Dentro** = las reglas de negocio. Es la parte que seguiría siendo cierta sobre papel, sin ordenador: "una cancelación surte efecto al final del periodo pagado", "un cambio de plan cobra la diferencia prorrateada".
- **Fuera** = todo lo que es un detalle de entrega: HTTP, Kafka, PostgreSQL, Stripe, SendGrid, el reloj.

El dentro no debe saber cómo es el fuera. El fuera se adapta al dentro. No al revés.

```
             HTTP / Kafka / Cron
                     │
             ┌───────▼─────────┐
             │ adaptador entrada│   SubscriptionController
             └───────┬─────────┘
                     │ llama
             ┌───────▼─────────┐
             │  puerto entrada │   CancelSubscriptionUseCase
             └───────┬─────────┘
                     │ implementado por
             ┌───────▼─────────┐        ┌──────────────────┐
             │   aplicación    │───────▶│  puerto salida   │  SaveSubscriptionPort
             │   (caso de uso) │        └────────▲─────────┘
             └───────┬─────────┘                 │ implementado por
                     │ usa                     │
             ┌───────▼─────────┐        ┌───────┴──────────┐
             │ modelo de dominio│       │ adaptador salida │  SubscriptionPersistenceAdapter
             └─────────────────┘        └───────┬──────────┘
                                                │
                                        PostgreSQL / Stripe
```

Lee las flechas como **dependencias de código fuente**, no como llamadas en ejecución. En ejecución, la petición fluye hacia dentro y la llamada a la base de datos fluye hacia fuera. En compilación, todas las flechas apuntan **hacia dentro**. Esa inversión es la arquitectura. Todo lo demás es nomenclatura.

**¿Para qué molestarse?** Por tres beneficios concretos, no por estética:

1. **Las pruebas tardan milisegundos.** El dominio y los casos de uso no tienen contexto de Spring, ni base de datos, ni HTTP. Pruebas las reglas con JUnit a secas.
2. **La tecnología se vuelve reemplazable.** Cambiar Stripe por otro proveedor, o JPA por jOOQ, toca un adaptador, no las reglas de negocio.
3. **El código te dice dónde va cada cosa.** "¿Dónde pongo esta clase?" deja de ser cuestión de gusto. La regla de dependencia decide.

> **Resumen en una frase de la Parte 2:** un caso de uso es el *trabajo*, un puerto es la *interfaz*, un adaptador es la *tecnología*.

---

## 2. El vocabulario, con precisión

La Parte 2 define los términos rápido. Esta es la versión que necesitas cuando de verdad estás colocando archivos.

### 2.1 Caso de uso

Un caso de uso es **una intención observable por el usuario**, de principio a fin. No es una operación CRUD sobre una tabla, ni un método de una entidad.

- "Suscribirse a un plan" → `SubscribeUserUseCase`
- "Cancelar mi suscripción" → `CancelSubscriptionUseCase`
- "Cambiar de plan" → `ChangePlanUseCase`
- "Renovar las suscripciones que vencen hoy" → `RenewSubscriptionUseCase`

Una buena prueba: si puedes describírselo a un responsable de producto en una frase con un verbo y un resultado, es un caso de uso.

### 2.2 Puerto

Un puerto es una **interfaz propiedad del dentro**. Hay dos direcciones, y confundirlas es el error más habitual.

| | Puerto de entrada | Puerto de salida |
|---|---|---|
| Otros nombres | puerto primario, *driving port* | puerto secundario, *driven port* |
| Quién lo define | la aplicación | la aplicación (o el dominio) |
| Quién lo implementa | el servicio de aplicación | un adaptador |
| Quién lo llama | un adaptador de entrada (controlador, consumidor, tarea programada) | el servicio de aplicación |
| Responde a | "¿Qué me puede pedir el exterior?" | "¿Qué necesito del exterior?" |
| Ejemplo | `CancelSubscriptionUseCase` | `SaveSubscriptionPort` |

Dos detalles que el artículo no subraya:

- **El puerto de entrada es el caso de uso.** La Parte 2 lo dice, y conviene tomarlo al pie de la letra: no necesitas un `port/in/CancelSubscription` separado más un `usecase/CancelSubscription`. Una interfaz, llamada `...UseCase`, colocada en `application/port/in`.
- **Un puerto pertenece al lado del que llama, no al lado de la implementación.** `SaveSubscriptionPort` vive en la aplicación porque es la aplicación quien lo necesita. El adaptador JPA vive fuera y lo implementa. Esto es Inversión de Dependencias en estado puro: la interfaz está junto al código que la *usa*, no junto al código que la *implementa*.

### 2.3 Adaptador

Un adaptador es la **implementación atada a una tecnología** de un puerto. Traduce entre el lenguaje del exterior y el lenguaje del interior.

- Entrada: petición HTTP → objeto de comando. `SubscriptionController`.
- Salida: objeto de dominio → fila SQL. `SubscriptionPersistenceAdapter`.
- Salida: `PaymentRequest` → llamada a la API de Stripe. `StripePaymentAdapter`.

Un adaptador debería ser fino y aburrido. Si un adaptador contiene un `if` sobre política de negocio, ese `if` pertenece al dominio.

### 2.4 Servicio de aplicación frente a servicio de dominio

Esta distinción falta en la Parte 2 y provoca confusión real.

| | Servicio de aplicación | Servicio de dominio |
|---|---|---|
| Vive en | `application/service` | `domain/service` |
| Función | orquestar un caso de uso: cargar, decidir, llamar a puertos, guardar | contener una regla que abarca varios objetos de dominio |
| Conoce | puertos, transacciones, tiempo | solo objetos de dominio y Java |
| Ejemplo | `CancelSubscriptionService` | `ProrationPolicy` |
| Tiene E/S | sí, a través de puertos | nunca |

Un servicio de dominio no es un bean de Spring y no tiene repositorio. Si te encuentras inyectando un repositorio en una clase de `domain/service`, es un servicio de aplicación disfrazado.

### 2.5 El reparto completo

| Concepto | Papel | Nombre típico | Paquete |
|---|---|---|---|
| Entidad / agregado | estado + invariantes | `Subscription` | `domain/model` |
| Objeto de valor | concepto inmutable | `Money`, `BillingPeriod` | `domain/model` |
| Regla de dominio | cálculo puro entre objetos | `ProrationPolicy` | `domain/service` |
| Puerto de entrada | lo que la app ofrece | `CancelSubscriptionUseCase` | `application/port/in` |
| Comando / resultado | entrada y salida de un caso de uso | `CancelSubscriptionCommand`, `SubscriptionView` | `application/command`, `application/view` |
| Puerto de salida | lo que la app necesita | `SaveSubscriptionPort` | `application/port/out` |
| Servicio de aplicación | orquesta el caso de uso | `CancelSubscriptionService` | `application/service` |
| Adaptador de entrada | entrada HTTP / Kafka / cron | `SubscriptionController` | `adapter/in/web` |
| Adaptador de salida | SQL / cliente HTTP | `SubscriptionPersistenceAdapter` | `adapter/out/persistence` |
| Cableado | une puertos con adaptadores | `SubscriptionConfig` | `config` |

### 2.6 Nomenclatura y sus dialectos

La convención de la Parte 2 es buena: por el nombre de la clase sabes la capa y la dirección. Pero en el mundo real encontrarás otros dialectos. Describen lo mismo.

| Concepto | Parte 2 / esta guía | Alternativa habitual | En este repositorio |
|---|---|---|---|
| Puerto de entrada | `...UseCase` | `...Api`, `...Command` | `CatalogCommandService` (interfaz; las implementaciones llevan `@UseCase`) |
| Puerto de salida | `...Port` | `...Repository`, `...Gateway`, `...Spi` | `CatalogRepository` en `domain/port` |
| Implementación del caso de uso | `...Service` | `...UseCaseHandler`, `...Interactor` | `CatalogCommandServiceImpl` |
| Adaptador de entrada | `...Controller` | `...Handler`, `...Endpoint` | `api/...Controller` |
| Adaptador de salida | `...Adapter` | `...RepositoryImpl`, `...Client` | `infrastructure/persistence/Jpa...Adapter` |

Ninguno es incorrecto. Elige un dialecto por proyecto, escríbelo y deja que ArchUnit vigile la dirección aunque los nombres varíen.

---

## 3. La regla de dependencia (la única regla)

La Parte 2 la enuncia así:

> los adaptadores pueden depender de la aplicación; la aplicación puede depender del dominio.

Es correcto pero incompleto. La regla completa, incluida la dirección que de verdad vas a tener tentaciones de romper:

```
adapter/in ──▶ application ──▶ domain
adapter/out ─▶ application ──▶ domain
                    │
                    └────────▶ domain (también permitido)
domain ──▶ nada
```

- `domain` no importa **ningún framework**. Ni Spring, ni JPA, ni Jackson, ni Jakarta Validation. Java puro y el JDK.
- `application` importa `domain`, y puede importar Spring solo para cableado y transacciones (`@Service`, `@Transactional`). No debe importar JPA, HTTP ni un adaptador.
- `adapter` importa `application` **y** `domain` (necesita mapear objetos de dominio), además de lo que exija la tecnología.
- Nadie importa un adaptador salvo la configuración de Spring que lo instancia.

¿Por qué "la aplicación solo puede importar el dominio" y no "la aplicación no importa nada"? Porque el caso de uso necesita el agregado y los puertos. Ese es su material.

¿Por qué el dominio no importa nada? Porque en el momento en que `Subscription` lleva `@Entity` o `@JsonProperty`, ya no puedes probarla sin un framework, y su forma empieza a venir dictada por la base de datos o por el contrato de la API.

### 3.1 La parte sutil: que los adaptadores importen el dominio está bien

Una lectura equivocada habitual es "nada puede importar nada". No: `SubscriptionPersistenceAdapter` debe importar `Subscription`, porque convierte uno en otro. La dirección prohibida es la contraria: `Subscription` no debe importar nunca `SubscriptionJpaEntity`, y `CancelSubscriptionService` no debe importar nunca `SubscriptionPersistenceAdapter`.

### 3.2 Hacerla cumplir

Puedes prometer que tendrás cuidado. No lo tendrás. La Parte 8 de la serie presenta ArchUnit; no hay razón para esperar hasta entonces. Añádelo ya:

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <version>1.5.1</version>
  <scope>test</scope>
</dependency>
```

```java
package com.example.subscription.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.subscription",
                importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "com.fasterxml.jackson..",
                    "org.hibernate..")
            .because("el dominio debe poder ejecutarse en una prueba unitaria normal");

    @ArchTest
    static final ArchRule application_does_not_know_adapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("los casos de uso dependen de puertos, nunca de implementaciones");

    @ArchTest
    static final ArchRule inbound_adapters_do_not_talk_to_outbound_adapters = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("un controlador llama a un caso de uso; nunca llega directo a la base de datos");

    @ArchTest
    static final ArchRule every_use_case_implements_an_inbound_port = classes()
            .that().resideInAPackage("..application.service..")
            .and().haveSimpleNameEndingWith("Service")
            .should().implement(DescribedPredicate.describe(
                    "un puerto de entrada",
                    implementedInterface -> implementedInterface.getPackageName()
                            .equals("com.example.subscription.application.port.in")))
            .because("un caso de uso es la implementación de un contrato, no una clase suelta")
            .allowEmptyShould(true);
}
```

Dos notas prácticas:

- `allowEmptyShould(true)` evita que una regla falle antes de que exista el paquete que vigila. Tu repositorio ya lo hace exactamente por eso (`ArchitectureTest`).
- Escribe una prueba que **demuestre que la regla puede fallar**. Una regla que no puede fallar no es una regla. Tu repositorio también lo hace, con una clase de prueba que filtra a propósito un import de framework.

---

## 4. Estructura de paquetes: dos formas válidas

### 4.1 Forma A — por capas (el esqueleto del artículo)

```
com.example.subscription
├── domain
│   ├── model
│   │   ├── Subscription.java
│   │   ├── Plan.java
│   │   ├── BillingPeriod.java
│   │   └── Money.java
│   └── service
│       └── ProrationPolicy.java
├── application
│   ├── port
│   │   ├── in
│   │   │   ├── SubscribeUserUseCase.java
│   │   │   └── CancelSubscriptionUseCase.java
│   │   └── out
│   │       ├── LoadSubscriptionPort.java
│   │       ├── SaveSubscriptionPort.java
│   │       └── ChargePaymentPort.java
│   ├── command
│   ├── view
│   └── service
│       ├── SubscribeUserService.java
│       └── CancelSubscriptionService.java
├── adapter
│   ├── in
│   │   └── web
│   │       ├── SubscriptionController.java
│   │       ├── SubscriptionWebMapper.java
│   │       └── dto/
│   └── out
│       ├── persistence
│       │   ├── SubscriptionPersistenceAdapter.java
│       │   ├── SubscriptionJpaEntity.java
│       │   ├── SubscriptionJpaRepository.java
│       │   └── SubscriptionMapper.java
│       ├── payment
│       │   └── StripePaymentAdapter.java
│       └── notification
│           └── SendGridEmailAdapter.java
├── config
│   └── SubscriptionConfig.java
└── SubscriptionApplication.java
```

Es la forma más fácil de aprender y la que dibuja el artículo. Funciona bien cuando la app tiene un solo contexto delimitado, o cuando cada contexto delimitado es su propio módulo o servicio.

### 4.2 Forma B — por funcionalidad (lo que hace este repositorio)

```
com.anderk.ecommerce
├── catalog
│   ├── api                 ← adaptador de entrada (controladores, DTOs, mapper web)
│   ├── application         ← casos de uso + puertos de entrada + vistas
│   ├── domain              ← modelo, políticas, objetos de valor, puertos de salida
│   └── infrastructure      ← adaptadores de salida (JPA), cableado
├── lead
├── order
├── audit
├── auth
└── shared
```

Cada funcionalidad es un hexágono pequeño con los mismos cuatro cuadrantes. Fíjate en las diferencias de nombres:

- `api` en lugar de `adapter/in/web`
- `infrastructure` en lugar de `adapter/out`
- `domain/port` en lugar de `application/port/out` para los repositorios
- `application` contiene las interfaces de caso de uso directamente, no bajo `port/in`

Las dos formas son hexagonales. La regla de dependencia es idéntica; solo cambian los nombres de las carpetas.

**¿Cuál elegir?**

| Situación | Recomendación |
|---|---|
| Un contexto delimitado, equipo pequeño | Forma A (por capas). Menos carpetas, lección más clara. |
| Varias funcionalidades con reglas propias (catálogo, pedidos, leads) | Forma B (por funcionalidad). Mantiene juntos los cinco archivos de una funcionalidad. |
| Quieres cumplimiento en tiempo de compilación | Un módulo Maven/Gradle por contexto delimitado y, dentro, la Forma B. |

La Parte 2 recomienda un solo módulo al principio, y es acertado. Multi-módulo compra cumplimiento en compilación a cambio de complejidad de build. Añádelo cuando el equipo sea lo bastante grande como para que ArchUnit no baste, no antes.

### 4.3 ¿Dónde van las pruebas?

Refleja el paquete de producción, un paquete de prueba por capa:

```
src/test/java/com/example/subscription
├── domain/model/SubscriptionTest.java              ← JUnit puro, milisegundos
├── application/service/CancelSubscriptionServiceTest.java  ← fakes, sin Spring
├── adapter/in/web/SubscriptionControllerTest.java   ← @WebMvcTest
├── adapter/out/persistence/SubscriptionAdapterTest.java    ← @DataJpaTest + Testcontainers
└── architecture/HexagonalArchitectureTest.java      ← las reglas de arriba
```

La forma del árbol de pruebas te dice si la arquitectura es real. Si todas las pruebas necesitan `@SpringBootTest`, el dominio no está aislado, digan lo que digan las carpetas.

### 4.4 "¿Dónde va este archivo?" — flujo de decisión

```
¿Es una regla de negocio, un estado o un cálculo que seguiría siendo cierto sin ordenador?
├── sí → domain/model (un objeto) o domain/service (varios objetos)
└── no
    ├── ¿Es algo que el exterior puede pedirle a la app?
    │   └── sí → application/port/in (interfaz `...UseCase`)
    ├── ¿Es algo que la app necesita del exterior?
    │   └── sí → application/port/out (interfaz `...Port`)
    ├── ¿Es la orquestación paso a paso de un caso de uso?
    │   └── sí → application/service (`...Service`)
    └── ¿Habla HTTP, SQL, Stripe, SMTP o Kafka?
        └── sí → adapter/in o adapter/out
```

Una segunda prueba que atrapa la mayoría de errores:

- ¿Puedo ejecutarlo desde un `main()` normal sin Spring? → pertenece a `domain`.
- ¿Puedo sustituirlo por un fake en memoria sin tocar el caso de uso? → es un puerto de salida.
- ¿Contiene la palabra `Request`, `Response`, `Entity` o `Table`? → es un detalle de adaptador y no debe cruzar la frontera.

---

## 5. El ejemplo, de principio a fin

### 5.1 El segmento que vamos a construir

El ejemplo es el servicio de suscripciones de la serie, con sus cuatro reglas básicas:

1. Un usuario puede suscribirse a un plan.
2. La suscripción se renueva al inicio de cada periodo de facturación; si el pago falla, queda suspendida.
3. El usuario puede cancelar en cualquier momento; la cancelación surte efecto en el siguiente periodo.
4. Cuando cambia el plan, la diferencia de precio se calcula de forma prorrateada.

Los casos de uso:

| Caso de uso | Puerto de entrada | Puertos de salida que necesita |
|---|---|---|
| Suscribirse | `SubscribeUserUseCase` | cargar, guardar, cobrar |
| Cancelar | `CancelSubscriptionUseCase` | cargar, guardar |
| Cambiar de plan | `ChangePlanUseCase` | cargar, guardar, cobrar |
| Renovar (programado) | `RenewSubscriptionUseCase` | cargar, guardar, cobrar, correo |

Dependencias: Spring Boot (web, data-jpa, validation), una base de datos, JUnit 5, AssertJ y ArchUnit. Nada más. Sin Lombok y sin MapStruct, para que el mapeo quede a la vista.

> **Una nota sobre Spring Boot 4.** En Boot 4 el starter web es `spring-boot-starter-webmvc`; en Boot 3 es `spring-boot-starter-web`. El código de abajo es independiente de la versión.

### 5.2 Dominio: Java puro, sin Spring

Esta es la parte que cubrirá la Parte 3 de la serie. Se incluye aquí para que el segmento compile y porque la forma del dominio dicta la forma de los puertos.

#### `domain/model/Money.java`

```java
package com.example.subscription.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money times(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isNegative() {
        return this.amount.signum() < 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "No se pueden combinar %s con %s".formatted(this.currency, other.currency));
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }
}
```

Por qué un `record` con constructor compacto: la invariante (dos decimales, misma moneda) se aplica en un solo sitio y ningún llamador puede crear un `Money` inválido. Fíjate en que no hay ni un import de framework. No es casualidad; es la regla del §3.

#### `domain/model/BillingPeriod.java`

```java
package com.example.subscription.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Un periodo de facturación, con ambos extremos incluidos.
 */
public record BillingPeriod(LocalDate start, LocalDate end) {

    public BillingPeriod {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("El periodo debe terminar después de empezar");
        }
    }

    public static BillingPeriod startingOn(LocalDate start) {
        return new BillingPeriod(start, start.plusMonths(1).minusDays(1));
    }

    public BillingPeriod next() {
        return startingOn(this.end.plusDays(1));
    }

    public int totalDays() {
        return (int) ChronoUnit.DAYS.between(this.start, this.end) + 1;
    }

    public int remainingDays(LocalDate today) {
        if (today.isBefore(this.start)) {
            return totalDays();
        }
        if (today.isAfter(this.end)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(today, this.end) + 1;
    }
}
```

#### `domain/model/Plan.java` y los identificadores

```java
package com.example.subscription.domain.model;

public record Plan(String code, String name, Money monthlyPrice) {

    public Plan {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Un plan necesita un código");
        }
    }
}
```

```java
package com.example.subscription.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(UUID value) {
    public SubscriptionId {
        Objects.requireNonNull(value, "value");
    }

    public static SubscriptionId newId() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
```

```java
package com.example.subscription.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "value");
    }
}
```

El identificador se genera **dentro** del dominio, antes de que lo vea la base de datos. Eso elimina el problema del "¿qué ID tiene antes de guardar?", mantiene el agregado válido desde la primera línea y permite que el adaptador de pago use el mismo ID como clave de idempotencia. Si tu base de datos exige `BIGSERIAL`, puedes mantener el ID de dominio como UUID aparte o aceptar un ID nulo hasta el guardado. Elige una opción y sé consistente.

#### `domain/service/ProrationPolicy.java`

```java
package com.example.subscription.domain.service;

import com.example.subscription.domain.model.BillingPeriod;
import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.Plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * La regla del dinero que se debe cuando el plan cambia a mitad de periodo.
 *
 * <p>Cálculo puro: sin repositorio, sin inyección de reloj, sin Spring. Es un
 * servicio de dominio porque necesita tres objetos (dos planes y un periodo) y
 * no pertenece a ninguno de ellos.
 *
 * @return una cantidad positiva a cobrar, una negativa a abonar
 */
public final class ProrationPolicy {

    private ProrationPolicy() {
    }

    public static Money difference(Plan current, Plan next, BillingPeriod period, LocalDate today) {
        int remaining = period.remainingDays(today);
        int total = period.totalDays();
        if (remaining == 0) {
            return Money.zero(current.monthlyPrice().currency());
        }

        BigDecimal ratio = BigDecimal.valueOf(remaining)
                .divide(BigDecimal.valueOf(total), 10, RoundingMode.HALF_UP);

        return next.monthlyPrice()
                .minus(current.monthlyPrice())
                .times(ratio);
    }
}
```

Un servicio de dominio no es un `@Service` de Spring. Es solo una clase con una función pura. En el momento en que la anotas y le inyectas un repositorio, deja de ser un servicio de dominio.

#### `domain/model/Subscription.java` — el agregado

```java
package com.example.subscription.domain.model;

import com.example.subscription.domain.exception.SamePlanException;
import com.example.subscription.domain.exception.SubscriptionAlreadyCancelledException;
import com.example.subscription.domain.exception.SubscriptionNotActiveException;
import com.example.subscription.domain.service.ProrationPolicy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * La raíz del agregado: todo cambio de una suscripción pasa por esta clase.
 */
public final class Subscription {

    private final SubscriptionId id;
    private final UserId userId;
    private Plan plan;
    private SubscriptionStatus status;
    private BillingPeriod period;
    private boolean cancelAtPeriodEnd;

    private Subscription(SubscriptionId id, UserId userId, Plan plan,
                         SubscriptionStatus status, BillingPeriod period,
                         boolean cancelAtPeriodEnd) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.plan = Objects.requireNonNull(plan);
        this.status = Objects.requireNonNull(status);
        this.period = Objects.requireNonNull(period);
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    /** Una suscripción nueva, activa desde hoy. */
    public static Subscription subscribe(UserId userId, Plan plan, LocalDate today) {
        return new Subscription(SubscriptionId.newId(), userId, plan,
                SubscriptionStatus.ACTIVE, BillingPeriod.startingOn(today), false);
    }

    /**
     * Reconstruye una suscripción desde el almacenamiento. Solo lo llama el
     * adaptador de persistencia; el código de negocio usa {@link #subscribe}.
     */
    public static Subscription rehydrate(SubscriptionId id, UserId userId, Plan plan,
                                         SubscriptionStatus status, BillingPeriod period,
                                         boolean cancelAtPeriodEnd) {
        return new Subscription(id, userId, plan, status, period, cancelAtPeriodEnd);
    }

    /**
     * Solicita la cancelación. La suscripción sigue ACTIVE y queda marcada; el
     * estado pasa a CANCELLED al inicio del siguiente periodo.
     */
    public void cancel() {
        requireActive();
        if (this.cancelAtPeriodEnd) {
            throw new SubscriptionAlreadyCancelledException(this.id);
        }
        this.cancelAtPeriodEnd = true;
    }

    /**
     * Aplica una renovación exitosa: o empieza el siguiente periodo o, si el
     * usuario había cancelado, la suscripción termina aquí.
     */
    public void renew() {
        requireActive();
        if (this.cancelAtPeriodEnd) {
            this.status = SubscriptionStatus.CANCELLED;
            return;
        }
        this.period = this.period.next();
    }

    /** El pago falló: la suscripción queda congelada hasta que se pague. */
    public void suspend() {
        if (this.status == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionNotActiveException(this.id);
        }
        this.status = SubscriptionStatus.SUSPENDED;
    }

    public void reactivate() {
        if (this.status != SubscriptionStatus.SUSPENDED) {
            throw new SubscriptionNotActiveException(this.id);
        }
        this.status = SubscriptionStatus.ACTIVE;
    }

    /**
     * Cambia a otro plan y devuelve la diferencia prorrateada: positiva para
     * cobrar, negativa para abonar.
     */
    public Money changePlan(Plan nextPlan, LocalDate today) {
        requireActive();
        if (this.plan.code().equals(nextPlan.code())) {
            throw new SamePlanException(this.plan.code());
        }
        Money difference = ProrationPolicy.difference(this.plan, nextPlan, this.period, today);
        this.plan = nextPlan;
        return difference;
    }

    private void requireActive() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionNotActiveException(this.id);
        }
    }

    public SubscriptionId id() {
        return this.id;
    }

    public UserId userId() {
        return this.userId;
    }

    public Plan plan() {
        return this.plan;
    }

    public SubscriptionStatus status() {
        return this.status;
    }

    public BillingPeriod period() {
        return this.period;
    }

    public boolean cancelAtPeriodEnd() {
        return this.cancelAtPeriodEnd;
    }

    public Money amountDue() {
        return this.plan.monthlyPrice();
    }
}
```

```java
package com.example.subscription.domain.model;

public enum SubscriptionStatus {
    ACTIVE, SUSPENDED, CANCELLED
}
```

Tres cosas que conviene notar:

1. **Sin setters.** Cada cambio de estado es un método con nombre y una regla asociada. Eso es lo que significa "modelo de dominio rico" en la práctica.
2. **Sin `@Entity`, sin `@Id`, sin Jackson.** La clase se puede probar en un JUnit normal, como muestra la sección siguiente.
3. **`rehydrate` existe para el adaptador.** Es un factory estático, no un constructor público de cinco argumentos. Documenta su propio propósito.

#### Las excepciones de dominio

```java
package com.example.subscription.domain.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(Object id) {
        super("No se encontró la suscripción %s".formatted(id));
    }
}
```

```java
package com.example.subscription.domain.exception;

public class SubscriptionNotActiveException extends RuntimeException {
    public SubscriptionNotActiveException(Object id) {
        super("La suscripción %s no está activa".formatted(id));
    }
}
```

```java
package com.example.subscription.domain.exception;

public class SubscriptionAlreadyCancelledException extends RuntimeException {
    public SubscriptionAlreadyCancelledException(Object id) {
        super("La suscripción %s ya está programada para cancelación".formatted(id));
    }
}
```

```java
package com.example.subscription.domain.exception;

public class SamePlanException extends RuntimeException {
    public SamePlanException(String planCode) {
        super("La suscripción ya está en el plan " + planCode);
    }
}
```

```java
package com.example.subscription.domain.exception;

import com.example.subscription.domain.model.SubscriptionId;

public class SubscriptionAlreadyExistsException extends RuntimeException {
    public SubscriptionAlreadyExistsException(SubscriptionId id) {
        super("Ya existe la suscripción %s para este usuario".formatted(id.value()));
    }
}
```

```java
package com.example.subscription.domain.exception;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(String reason) {
        super("No se pudo completar el pago: " + reason);
    }
}
```

Las excepciones de dominio forman parte del lenguaje del dominio. No llevan estado HTTP ni JSON. El adaptador web decide más tarde que "no encontrado" significa 404.

`PaymentFailedException` es discutiblemente una preocupación de aplicación y no de dominio, porque el dominio no tiene ni idea de que los pagos existen como sistema externo. Mantenerla junto a las excepciones de dominio es una decisión pragmática; si prefieres una separación estricta, ponla en `application/exception`. Lo que importa es que nunca transporte datos específicos del proveedor.

#### La prueba de dominio — sin Spring, sin base de datos

```java
package com.example.subscription.domain.model;

import com.example.subscription.domain.exception.SubscriptionAlreadyCancelledException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);
    private static final Plan BASIC = new Plan("basic", "Basic", Money.of("9.99", "EUR"));
    private static final Plan PRO = new Plan("pro", "Pro", Money.of("29.99", "EUR"));
    private static final UserId USER = new UserId(java.util.UUID.randomUUID());

    @Test
    void cancellation_takes_effect_at_the_next_period() {
        Subscription subscription = Subscription.subscribe(USER, BASIC, TODAY);

        subscription.cancel();

        assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.cancelAtPeriodEnd()).isTrue();

        subscription.renew();

        assertThat(subscription.status()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    void cancelling_twice_is_rejected() {
        Subscription subscription = Subscription.subscribe(USER, BASIC, TODAY);
        subscription.cancel();

        assertThatThrownBy(subscription::cancel)
                .isInstanceOf(SubscriptionAlreadyCancelledException.class);
    }

    @Test
    void upgrading_mid_period_charges_the_prorated_difference() {
        Subscription subscription = Subscription.subscribe(USER, BASIC, TODAY);
        LocalDate halfway = TODAY.plusDays(15);   // 16 de enero: quedan 16 de 31 días

        Money difference = subscription.changePlan(PRO, halfway);

        // (29.99 - 9.99) * 16 / 31 = 10.32
        assertThat(difference.amount()).isEqualByComparingTo("10.32");
        assertThat(subscription.plan()).isEqualTo(PRO);
    }
}
```

Esa prueba se ejecuta en unos milisegundos. Sin `@SpringBootTest`, sin H2, sin Docker. Este es el beneficio que promete el artículo; aquí está la prueba.

### 5.3 Puertos de salida

Los puertos describen **lo que el caso de uso necesita**, en el lenguaje del dominio. Son interfaces y viven junto al código que las llama.

#### `application/port/out/LoadSubscriptionPort.java`

```java
package com.example.subscription.application.port.out;

import com.example.subscription.domain.model.Subscription;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.UserId;

import java.util.Optional;

public interface LoadSubscriptionPort {

    Optional<Subscription> byId(SubscriptionId id);

    Optional<Subscription> byUserId(UserId userId);
}
```

#### `application/port/out/SaveSubscriptionPort.java`

```java
package com.example.subscription.application.port.out;

import com.example.subscription.domain.model.Subscription;

public interface SaveSubscriptionPort {

    Subscription save(Subscription subscription);
}
```

La Parte 2 los llama `LoadSubscriptionPort` y `SaveSubscriptionPort` y describe el patrón como "un puerto por capacidad". Es un buen valor por defecto. Puedes fusionarlos en un único `SubscriptionRepository` si siempre los implementa el mismo adaptador y ningún caso de uso necesita solo uno. No los fusiones solo porque golpeen la misma tabla; sepáralos porque son promesas distintas.

#### `application/port/out/ChargePaymentPort.java`

```java
package com.example.subscription.application.port.out;

import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.UserId;

public interface ChargePaymentPort {

    PaymentResult charge(PaymentRequest request);
}
```

```java
package com.example.subscription.application.port.out;

import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.UserId;

public record PaymentRequest(SubscriptionId subscriptionId, UserId userId, Money amount) {
}
```

```java
package com.example.subscription.application.port.out;

public record PaymentResult(boolean successful, String reference, String failureReason) {

    public static PaymentResult ok(String reference) {
        return new PaymentResult(true, reference, null);
    }

    public static PaymentResult failed(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
```

Los records de petición y resultado del puerto viven **junto al puerto**, no en el adaptador. Forman parte del contrato. `PaymentResult` deliberadamente no tiene tipos de Stripe: si los tuviera, la aplicación dependería de Stripe.

### 5.4 Puertos de entrada (casos de uso)

#### `application/command/CancelSubscriptionCommand.java`

```java
package com.example.subscription.application.command;

import java.util.UUID;

public record CancelSubscriptionCommand(UUID subscriptionId) {
}
```

#### `application/port/in/CancelSubscriptionUseCase.java`

```java
package com.example.subscription.application.port.in;

import com.example.subscription.application.command.CancelSubscriptionCommand;

public interface CancelSubscriptionUseCase {

    void cancel(CancelSubscriptionCommand command);
}
```

#### `application/port/in/SubscribeUserUseCase.java`

```java
package com.example.subscription.application.port.in;

import com.example.subscription.application.command.SubscribeCommand;
import com.example.subscription.application.view.SubscriptionView;

public interface SubscribeUserUseCase {

    SubscriptionView subscribe(SubscribeCommand command);
}
```

```java
package com.example.subscription.application.command;

import com.example.subscription.domain.model.Plan;
import com.example.subscription.domain.model.UserId;

public record SubscribeCommand(UserId userId, Plan plan) {
}
```

#### `application/view/SubscriptionView.java`

```java
package com.example.subscription.application.view;

import com.example.subscription.domain.model.Subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lo que el exterior ve de una suscripción. No es el agregado: es un modelo de
 * lectura, libre de cambiar de forma sin tocar el dominio.
 */
public record SubscriptionView(UUID id,
                               UUID userId,
                               String planCode,
                               String status,
                               LocalDate periodStart,
                               LocalDate periodEnd,
                               boolean cancelAtPeriodEnd,
                               BigDecimal amount) {

    public static SubscriptionView from(Subscription subscription) {
        return new SubscriptionView(
                subscription.id().value(),
                subscription.userId().value(),
                subscription.plan().code(),
                subscription.status().name(),
                subscription.period().start(),
                subscription.period().end(),
                subscription.cancelAtPeriodEnd(),
                subscription.amountDue().amount());
    }
}
```

¿Por qué no devolver el `Subscription` directamente? Podrías. Pero entonces el contrato de la API pasa a ser la forma del agregado para siempre, y cualquier refactor del modelo rompe a los clientes. Un `record` de vista cuesta diez líneas y te compra libertad. Tu repositorio hace lo mismo con `CatalogItemDetail`.

### 5.5 Servicios de aplicación

El servicio de aplicación es el director de orquesta. Carga el agregado, llama a un método de dominio, llama a los puertos y guarda. No contiene **reglas de negocio** (esas están en el dominio) ni **tecnología** (esa está en los adaptadores).

#### `application/service/CancelSubscriptionService.java`

```java
package com.example.subscription.application.service;

import com.example.subscription.application.command.CancelSubscriptionCommand;
import com.example.subscription.application.port.in.CancelSubscriptionUseCase;
import com.example.subscription.application.port.out.LoadSubscriptionPort;
import com.example.subscription.application.port.out.SaveSubscriptionPort;
import com.example.subscription.domain.exception.SubscriptionNotFoundException;
import com.example.subscription.domain.model.Subscription;
import com.example.subscription.domain.model.SubscriptionId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CancelSubscriptionService implements CancelSubscriptionUseCase {

    private final LoadSubscriptionPort subscriptions;
    private final SaveSubscriptionPort saves;

    public CancelSubscriptionService(LoadSubscriptionPort subscriptions, SaveSubscriptionPort saves) {
        this.subscriptions = subscriptions;
        this.saves = saves;
    }

    @Override
    public void cancel(CancelSubscriptionCommand command) {
        Subscription subscription = this.subscriptions.byId(new SubscriptionId(command.subscriptionId()))
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

        subscription.cancel();

        this.saves.save(subscription);
    }
}
```

Lee ese método en voz alta: *buscar la suscripción, cancelarla, guardarla*. Ese es el caso de uso, y el código dice exactamente eso. Sin SQL, sin HTTP, sin Stripe, sin ruido de logs.

El campo de dependencia está tipado como el **puerto**, no como el adaptador. `CancelSubscriptionService` nunca ha oído hablar de `SubscriptionPersistenceAdapter`, y eso es lo que hace posible la prueba del §5.10.

#### `application/service/SubscribeUserService.java`

```java
package com.example.subscription.application.service;

import com.example.subscription.application.command.SubscribeCommand;
import com.example.subscription.application.port.in.SubscribeUserUseCase;
import com.example.subscription.application.port.out.ChargePaymentPort;
import com.example.subscription.application.port.out.LoadSubscriptionPort;
import com.example.subscription.application.port.out.PaymentRequest;
import com.example.subscription.application.port.out.PaymentResult;
import com.example.subscription.application.port.out.SaveSubscriptionPort;
import com.example.subscription.application.view.SubscriptionView;
import com.example.subscription.domain.exception.PaymentFailedException;
import com.example.subscription.domain.exception.SubscriptionAlreadyExistsException;
import com.example.subscription.domain.model.Subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class SubscribeUserService implements SubscribeUserUseCase {

    private final LoadSubscriptionPort subscriptions;
    private final SaveSubscriptionPort saves;
    private final ChargePaymentPort payments;
    private final Clock clock;

    public SubscribeUserService(LoadSubscriptionPort subscriptions,
                                SaveSubscriptionPort saves,
                                ChargePaymentPort payments,
                                Clock clock) {
        this.subscriptions = subscriptions;
        this.saves = saves;
        this.payments = payments;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SubscriptionView subscribe(SubscribeCommand command) {
        this.subscriptions.byUserId(command.userId())
                .ifPresent(existing -> {
                    throw new SubscriptionAlreadyExistsException(existing.id());
                });

        LocalDate today = LocalDate.now(this.clock);
        Subscription subscription = Subscription.subscribe(command.userId(), command.plan(), today);

        // Nos salimos de la transacción: cobrar es una llamada HTTP al proveedor.
        // El adaptador abre una transacción corta para la lectura y otra para el
        // guardado, así que no se retiene una conexión mientras Stripe piensa.
        // Ver §6.7 para la alternativa PENDING → cobrar → confirmar.
        PaymentResult payment = this.payments.charge(new PaymentRequest(
                subscription.id(), subscription.userId(), subscription.amountDue()));

        if (!payment.successful()) {
            throw new PaymentFailedException(payment.failureReason());
        }

        return SubscriptionView.from(this.saves.save(subscription));
    }
}
```

Fíjate en lo que **no** hay aquí: ni `ResponseEntity`, ni `@RequestBody`, ni `if (user.isAdmin())`, ni `try/catch (StripeException)`. Todo eso pertenece a un adaptador.

#### El reloj

`Clock` es un tipo del JDK, así que inyectarlo no viola la regla de dependencia. Te da pruebas deterministas:

```java
Clock fixed = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
```

Nunca llames a `LocalDate.now()` dentro del dominio. El tiempo es una entrada, no un hecho ambiental.

### 5.6 El adaptador web

#### `adapter/in/web/dto/SubscribeRequest.java`

```java
package com.example.subscription.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(@NotBlank String planCode) {
}
```

#### `adapter/in/web/SubscriptionController.java`

```java
package com.example.subscription.adapter.in.web;

import com.example.subscription.adapter.in.web.dto.SubscribeRequest;
import com.example.subscription.application.command.CancelSubscriptionCommand;
import com.example.subscription.application.port.in.CancelSubscriptionUseCase;
import com.example.subscription.application.port.in.SubscribeUserUseCase;
import com.example.subscription.application.view.SubscriptionView;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscribeUserUseCase subscribe;
    private final CancelSubscriptionUseCase cancel;

    public SubscriptionController(SubscribeUserUseCase subscribe, CancelSubscriptionUseCase cancel) {
        this.subscribe = subscribe;
        this.cancel = cancel;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionView subscribe(@Valid @RequestBody SubscribeRequest request,
                                      @RequestHeader("X-User-Id") UUID userId) {
        return this.subscribe.subscribe(SubscriptionWebMapper.toCommand(request, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        this.cancel.cancel(new CancelSubscriptionCommand(id));
    }
}
```

Dos decisiones de diseño:

- Los campos del controlador son **interfaces de caso de uso**. Si mañana `SubscribeUserService` se divide en dos, el controlador no cambia.
- `PlanCatalog` es un sustituto del catálogo de planes. En un sistema real, el plan se carga con un `LoadPlanPort`; aquí se abrevia para que el segmento se lea.

#### `adapter/in/web/SubscriptionWebMapper.java`

```java
package com.example.subscription.adapter.in.web;

import com.example.subscription.adapter.in.web.dto.SubscribeRequest;
import com.example.subscription.application.command.SubscribeCommand;
import com.example.subscription.domain.model.UserId;

import java.util.UUID;

public final class SubscriptionWebMapper {

    private SubscriptionWebMapper() {
    }

    public static SubscribeCommand toCommand(SubscribeRequest request, UUID userId) {
        return new SubscribeCommand(new UserId(userId), PlanCatalog.byCode(request.planCode()));
    }
}
```

```java
package com.example.subscription.adapter.in.web;

import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.Plan;

import java.util.Map;

/**
 * Un sustituto del catálogo de planes. En una aplicación real el plan viene de
 * un LoadPlanPort; esto mantiene el segmento centrado en la arquitectura.
 */
final class PlanCatalog {

    private static final Map<String, Plan> PLANS = Map.of(
            "basic", new Plan("basic", "Basic", Money.of("9.99", "EUR")),
            "pro", new Plan("pro", "Pro", Money.of("29.99", "EUR")));

    private PlanCatalog() {
    }

    static Plan byCode(String code) {
        Plan plan = PLANS.get(code);
        if (plan == null) {
            throw new IllegalArgumentException("Plan desconocido: " + code);
        }
        return plan;
    }
}
```

Los mappers son aburridos a propósito. Mantenlos estáticos, mantenlos en el adaptador y no dejes que un `...Request` llegue nunca a la capa de aplicación.

#### `adapter/in/web/ApiExceptionHandler.java`

```java
package com.example.subscription.adapter.in.web;

import com.example.subscription.domain.exception.PaymentFailedException;
import com.example.subscription.domain.exception.SamePlanException;
import com.example.subscription.domain.exception.SubscriptionAlreadyCancelledException;
import com.example.subscription.domain.exception.SubscriptionNotFoundException;
import com.example.subscription.domain.exception.SubscriptionNotActiveException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SubscriptionNotFoundException.class)
    ProblemDetail notFound(SubscriptionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            SubscriptionAlreadyCancelledException.class,
            SubscriptionNotActiveException.class,
            SamePlanException.class
    })
    ProblemDetail conflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    ProblemDetail paymentFailed(PaymentFailedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }
}
```

Este es el único sitio que conoce la correspondencia entre fallos de dominio y códigos HTTP. Es exactamente el tipo de conocimiento que no debe filtrarse hacia dentro.

### 5.7 El adaptador de persistencia

Aquí es donde la mayoría de las implementaciones se tuercen. La regla: **dos modelos, un mapper, cero filtraciones**.

#### `adapter/out/persistence/SubscriptionJpaEntity.java`

```java
package com.example.subscription.adapter.out.persistence;

import com.example.subscription.domain.model.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
class SubscriptionJpaEntity {

    // Campos package-private: Hibernate usa acceso por campo (porque @Id está
    // en el campo) y el mapper vive en este paquete. No hacen falta getters, y
    // nada fuera del adaptador puede ver esta clase.
    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "plan_code", nullable = false)
    String planCode;

    @Column(name = "plan_name", nullable = false)
    String planName;

    @Column(name = "plan_price", nullable = false, precision = 12, scale = 2)
    BigDecimal planPrice;

    @Column(nullable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SubscriptionStatus status;

    @Column(name = "period_start", nullable = false)
    LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    LocalDate periodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    boolean cancelAtPeriodEnd;

    @Version
    long version;

    protected SubscriptionJpaEntity() {
        // para JPA
    }

    SubscriptionJpaEntity(UUID id, UUID userId, String planCode, String planName,
                          BigDecimal planPrice, String currency, SubscriptionStatus status,
                          LocalDate periodStart, LocalDate periodEnd, boolean cancelAtPeriodEnd) {
        this.id = id;
        this.userId = userId;
        this.planCode = planCode;
        this.planName = planName;
        this.planPrice = planPrice;
        this.currency = currency;
        this.status = status;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }
}
```

#### `adapter/out/persistence/SubscriptionJpaRepository.java`

```java
package com.example.subscription.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    Optional<SubscriptionJpaEntity> findByUserId(UUID userId);
}
```

Es pública solo porque la clase de configuración y Spring Data necesitan verla. Sigue siendo un detalle del adaptador: la regla de ArchUnit del §3.2 prohíbe que `application` importe nada bajo `..adapter..`. Si quieres que el compilador también lo imponga, mueve el cableado a este paquete y vuelve a hacer la interfaz package-private: es un intercambio defendible, a costa de dispersar clases `@Configuration`.

Si un servicio inyecta esta interfaz directamente, la arquitectura ya está rota, diga lo que diga su visibilidad.

#### `adapter/out/persistence/SubscriptionMapper.java`

```java
package com.example.subscription.adapter.out.persistence;

import com.example.subscription.domain.model.BillingPeriod;
import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.Plan;
import com.example.subscription.domain.model.Subscription;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.SubscriptionStatus;
import com.example.subscription.domain.model.UserId;

import java.util.Currency;

final class SubscriptionMapper {

    private SubscriptionMapper() {
    }

    static Subscription toDomain(SubscriptionJpaEntity entity) {
        return Subscription.rehydrate(
                new SubscriptionId(entity.id),
                new UserId(entity.userId),
                new Plan(entity.planCode, entity.planName,
                        new Money(entity.planPrice, Currency.getInstance(entity.currency))),
                entity.status,
                new BillingPeriod(entity.periodStart, entity.periodEnd),
                entity.cancelAtPeriodEnd);
    }

    static SubscriptionJpaEntity toNewEntity(Subscription subscription) {
        return new SubscriptionJpaEntity(
                subscription.id().value(),
                subscription.userId().value(),
                subscription.plan().code(),
                subscription.plan().name(),
                subscription.amountDue().amount(),
                subscription.amountDue().currency().getCurrencyCode(),
                subscription.status(),
                subscription.period().start(),
                subscription.period().end(),
                subscription.cancelAtPeriodEnd());
    }

    static void copyInto(SubscriptionJpaEntity entity, Subscription subscription) {
        entity.planCode = subscription.plan().code();
        entity.planName = subscription.plan().name();
        entity.planPrice = subscription.amountDue().amount();
        entity.currency = subscription.amountDue().currency().getCurrencyCode();
        entity.status = subscription.status();
        entity.periodStart = subscription.period().start();
        entity.periodEnd = subscription.period().end();
        entity.cancelAtPeriodEnd = subscription.cancelAtPeriodEnd();
    }
}
```

#### `adapter/out/persistence/SubscriptionPersistenceAdapter.java`

```java
package com.example.subscription.adapter.out.persistence;

import com.example.subscription.application.port.out.LoadSubscriptionPort;
import com.example.subscription.application.port.out.SaveSubscriptionPort;
import com.example.subscription.domain.model.Subscription;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.UserId;

import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort {

    private final SubscriptionJpaRepository subscriptions;

    public SubscriptionPersistenceAdapter(SubscriptionJpaRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Optional<Subscription> byId(SubscriptionId id) {
        return this.subscriptions.findById(id.value()).map(SubscriptionMapper::toDomain);
    }

    @Override
    public Optional<Subscription> byUserId(UserId userId) {
        return this.subscriptions.findByUserId(userId.value()).map(SubscriptionMapper::toDomain);
    }

    @Override
    public Subscription save(Subscription subscription) {
        SubscriptionJpaEntity entity = this.subscriptions.findById(subscription.id().value())
                .map(existing -> {
                    SubscriptionMapper.copyInto(existing, subscription);
                    return existing;
                })
                .orElseGet(() -> SubscriptionMapper.toNewEntity(subscription));

        return SubscriptionMapper.toDomain(this.subscriptions.save(entity));
    }
}
```

¿Por qué no `save()` con `merge()` directamente? Porque reconstruir la entidad en cada guardado pierde la versión del bloqueo optimista y convierte una actualización rastreada en un `merge` de una entidad separada. Cargar y copiar mantiene el *dirty checking* de Hibernate, el `@Version` y la auditoría tipo Envers. Tu repositorio hace lo mismo en `JpaCatalogRepositoryAdapter`.

#### ¿Por qué dos modelos?

| | `Subscription` | `SubscriptionJpaEntity` |
|---|---|---|
| Conoce JPA | no | sí |
| Tiene `@Id`, `@Version` | no | sí |
| Necesita constructor sin argumentos | no | sí |
| Aplica invariantes | sí | no |
| Es mutable | solo mediante métodos | mediante setters |
| Lo usan | casos de uso, pruebas de dominio | Hibernate |

La duplicación es el precio de la independencia. En la práctica son unas decenas de líneas por agregado, y el mapper es mecánico. Si el agregado es una bolsa de datos anémica sin reglas, los dos modelos son puro sobrecoste, lo cual es una pista de que ese agregado no necesita ser un agregado.

**Un atajo tentador que hay que evitar:** anotar la clase de dominio como `@Entity` y llamarlo hexagonal. Compila, pero significa que los proxies de Hibernate, la carga perezosa y `@Version` forman parte de tu objeto de negocio. Pierdes las pruebas en milisegundos y la libertad de cambiar el esquema. Esto es lo que más separa "hexagonal en los nombres de carpetas" de "hexagonal de verdad".

### 5.8 Adaptadores externos: pago y correo

#### `adapter/out/payment/StripePaymentAdapter.java`

```java
package com.example.subscription.adapter.out.payment;

import com.example.subscription.application.port.out.ChargePaymentPort;
import com.example.subscription.application.port.out.PaymentRequest;
import com.example.subscription.application.port.out.PaymentResult;

public class StripePaymentAdapter implements ChargePaymentPort {

    private final StripeClient stripe;

    public StripePaymentAdapter(StripeClient stripe) {
        this.stripe = stripe;
    }

    @Override
    public PaymentResult charge(PaymentRequest request) {
        try {
            Receipt receipt = this.stripe.charge(
                    request.userId().value().toString(),
                    request.amount().amount(),
                    request.amount().currency().getCurrencyCode(),
                    // El id de la suscripción hace seguros los reintentos: Stripe
                    // ignora un segundo cobro con la misma clave.
                    "%s:%s".formatted(request.subscriptionId().value(), request.amount().amount()));

            return PaymentResult.ok(receipt.id());
        } catch (StripeException ex) {
            return PaymentResult.failed(ex.getMessage());
        }
    }
}
```

`StripeClient`, `Receipt` y `StripeException` representan lo que exponga el SDK que uses. Lo importante es la traducción:

- el puerto habla `PaymentRequest` / `PaymentResult`
- el adaptador habla Stripe
- ningún tipo de Stripe cruza la línea del `implements`

Fíjate también en la **clave de idempotencia**. Es una decisión relevante para el negocio (un reintento no debe cobrar dos veces) expresada dentro del adaptador. El caso de uso no necesita saber cómo lo consigue el proveedor.

#### `adapter/out/notification/SendGridEmailAdapter.java`

```java
package com.example.subscription.adapter.out.notification;

import com.example.subscription.application.port.out.SendEmailPort;

public class SendGridEmailAdapter implements SendEmailPort {

    private final SendGridClient sendGrid;

    public SendGridEmailAdapter(SendGridClient sendGrid) {
        this.sendGrid = sendGrid;
    }

    @Override
    public void send(String to, String subject, String body) {
        this.sendGrid.send(new SendGridMessage(to, subject, body));
    }
}
```

```java
package com.example.subscription.application.port.out;

public interface SendEmailPort {

    void send(String to, String subject, String body);
}
```

Dos adaptadores de salida, dos tecnologías, una capa de aplicación que no conoce ninguna de las dos.

### 5.9 Cablear el hexágono

Dos estilos. Ambos son válidos; elige uno por proyecto.

**Implícito (escaneo de componentes).** Anota el adaptador con `@Component` y deja que Spring lo encuentre:

```java
@Component
class SubscriptionPersistenceAdapter implements LoadSubscriptionPort, SaveSubscriptionPort {
    // ...
}
```

Spring lo inyecta donde se necesite un `LoadSubscriptionPort` o un `SaveSubscriptionPort`. Es menos código, pero el cableado queda invisible y una clase puede convertirse en bean por accidente.

**Explícito (una clase de configuración).** Es lo que hace tu repositorio, y es mejor para un hexágono:

```java
package com.example.subscription.config;

import com.example.subscription.adapter.out.notification.SendGridClient;
import com.example.subscription.adapter.out.notification.SendGridEmailAdapter;
import com.example.subscription.adapter.out.payment.StripeClient;
import com.example.subscription.adapter.out.payment.StripePaymentAdapter;
import com.example.subscription.adapter.out.persistence.SubscriptionJpaRepository;
import com.example.subscription.adapter.out.persistence.SubscriptionPersistenceAdapter;
import com.example.subscription.application.port.out.ChargePaymentPort;
import com.example.subscription.application.port.out.LoadSubscriptionPort;
import com.example.subscription.application.port.out.SaveSubscriptionPort;
import com.example.subscription.application.port.out.SendEmailPort;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionConfig {

    @Bean
    SubscriptionPersistenceAdapter subscriptionPersistenceAdapter(SubscriptionJpaRepository repository) {
        return new SubscriptionPersistenceAdapter(repository);
    }

    @Bean
    LoadSubscriptionPort loadSubscriptionPort(SubscriptionPersistenceAdapter adapter) {
        return adapter;
    }

    @Bean
    SaveSubscriptionPort saveSubscriptionPort(SubscriptionPersistenceAdapter adapter) {
        return adapter;
    }

    @Bean
    ChargePaymentPort chargePaymentPort(StripeClient stripe) {
        return new StripePaymentAdapter(stripe);
    }

    @Bean
    SendEmailPort sendEmailPort(SendGridClient sendGrid) {
        return new SendGridEmailAdapter(sendGrid);
    }
}
```

El truco de los tres beans importa. El adaptador implementa dos puertos. Si escribes `@Bean LoadSubscriptionPort load() { return new SubscriptionPersistenceAdapter(repo); }` y lo mismo para `save`, obtienes **dos instancias distintas**. Compartir una sola instancia mediante un `@Bean` al que delegan los dos beans de puerto mantiene el singleton y hace visible, en la configuración, la frase "un adaptador implementa dos puertos".

**Un estereotipo propio.** Tu repositorio da un paso más con una anotación `@UseCase` meta-anotada con `@Service` y `@Transactional`:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
@Transactional
public @interface UseCase {
}
```

Así cada caso de uso lleva `@UseCase`, es localizable con un grep, se escanea como bean y es transaccional por defecto. Una consulta de solo lectura lo sobrescribe con `@Transactional(readOnly = true)`. Es una convención pequeña y de alto valor: la anotación dice "esta clase es la implementación de un puerto de entrada" de un modo que `@Service` nunca podría.

### 5.10 Probar el segmento

La arquitectura solo es real si cambia cómo pruebas. Aquí está el segmento entero, capa por capa.

#### Prueba del caso de uso con fakes (sin Spring)

```java
package com.example.subscription.application.service;

import com.example.subscription.application.command.CancelSubscriptionCommand;
import com.example.subscription.application.port.out.LoadSubscriptionPort;
import com.example.subscription.application.port.out.SaveSubscriptionPort;
import com.example.subscription.domain.exception.SubscriptionNotFoundException;
import com.example.subscription.domain.model.Money;
import com.example.subscription.domain.model.Plan;
import com.example.subscription.domain.model.Subscription;
import com.example.subscription.domain.model.SubscriptionId;
import com.example.subscription.domain.model.UserId;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelSubscriptionServiceTest {

    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();
    private final CancelSubscriptionService service =
            new CancelSubscriptionService(subscriptions, subscriptions);

    @Test
    void cancels_an_existing_subscription() {
        Subscription subscription = this.subscriptions.save(Subscription.subscribe(
                new UserId(UUID.randomUUID()),
                new Plan("basic", "Basic", Money.of("9.99", "EUR")),
                LocalDate.of(2026, 1, 1)));

        this.service.cancel(new CancelSubscriptionCommand(subscription.id().value()));

        assertThat(this.subscriptions.byId(subscription.id()))
                .get()
                .extracting(Subscription::cancelAtPeriodEnd)
                .isEqualTo(true);
    }

    @Test
    void rejects_an_unknown_subscription() {
        assertThatThrownBy(() -> this.service.cancel(
                new CancelSubscriptionCommand(UUID.randomUUID())))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    /** Un fake, no un mock: se comporta como el de verdad, sin base de datos. */
    static final class InMemorySubscriptions implements LoadSubscriptionPort, SaveSubscriptionPort {

        private final Map<SubscriptionId, Subscription> rows = new HashMap<>();

        @Override
        public Optional<Subscription> byId(SubscriptionId id) {
            return Optional.ofNullable(this.rows.get(id));
        }

        @Override
        public Optional<Subscription> byUserId(UserId userId) {
            return this.rows.values().stream()
                    .filter(subscription -> subscription.userId().equals(userId))
                    .findFirst();
        }

        @Override
        public Subscription save(Subscription subscription) {
            this.rows.put(subscription.id(), subscription);
            return subscription;
        }
    }
}
```

Sin `@SpringBootTest`, sin `@MockBean`, sin H2. La prueba ejercita el servicio real y el agregado real. Se ejecuta en milisegundos.

Prefiere **fakes frente a mocks** en los puertos de salida. Un fake implementa el contrato y se puede reutilizar; un mock afirma sobre interacciones y acopla la prueba a la implementación. Usa un mock cuando necesites verificar un efecto secundario que no puedes observar de otra forma (por ejemplo, "se envió el correo exactamente una vez").

#### Prueba de porción web

```java
@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mvc;

    // Spring Boot 3.4+ y 4.x. En versiones anteriores, usa @MockBean.
    @MockitoBean
    private CancelSubscriptionUseCase cancel;

    @Test
    void returns_204_when_cancelled() throws Exception {
        this.mvc.perform(delete("/api/subscriptions/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(this.cancel).cancel(any(CancelSubscriptionCommand.class));
    }
}
```

Esta prueba conoce HTTP y el puerto, nada más. Si necesita la base de datos, es que el controlador hace demasiado.

#### Prueba de porción de persistencia

```java
@DataJpaTest
@Testcontainers
class SubscriptionPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private SubscriptionJpaRepository repository;

    @Test
    void round_trips_an_aggregate() {
        SubscriptionPersistenceAdapter adapter = new SubscriptionPersistenceAdapter(this.repository);
        Subscription saved = adapter.save(Subscription.subscribe(
                new UserId(UUID.randomUUID()),
                new Plan("basic", "Basic", Money.of("9.99", "EUR")),
                LocalDate.of(2026, 1, 1)));

        assertThat(adapter.byId(saved.id())).get()
                .extracting(subscription -> subscription.plan().code())
                .isEqualTo("basic");
    }
}
```

Usa un PostgreSQL real en Testcontainers, no H2, en cuanto tengas `jsonb`, enums o bloqueos. Tu repositorio ya aprendió esa lección con Hypersistence Utils.

---

## 6. Las preguntas que el artículo deja abiertas

### 6.1 ¿Dónde van los DTOs?

Hay **tres DTOs distintos**, y confundirlos es la fuente habitual de filtraciones.

| DTO | Pertenece a | Ejemplo | ¿Puede cruzar la frontera? |
|---|---|---|---|
| Petición/respuesta HTTP | `adapter/in/web/dto` | `SubscribeRequest` | no |
| Comando / vista | `application/command`, `application/view` | `SubscribeCommand`, `SubscriptionView` | sí: adaptador → aplicación |
| Entidad JPA | `adapter/out/persistence` | `SubscriptionJpaEntity` | no |

La regla: **un DTO de adaptador nunca viaja más allá del mapper de ese adaptador.** Un `SubscribeRequest` no debe aparecer en la firma de un caso de uso, y un `SubscriptionJpaEntity` no debe aparecer en un controlador.

### 6.2 ¿Dónde va `@Transactional`?

En el **servicio de aplicación**, alrededor del caso de uso. Ahí es donde empieza y termina un caso de uso, y por tanto donde empieza y termina la unidad de trabajo.

- No en el controlador: HTTP no es una frontera transaccional.
- No en el dominio: el dominio no debe saber que existen transacciones.
- En el adaptador, solo cuando el puerto se llama fuera de un caso de uso (una lectura en una prueba, un barrido programado). En esta guía el adaptador de persistencia lleva `@Transactional` para que sea seguro en ambos casos; el caso de uso sigue siendo la frontera principal.

Dos hechos de Spring que sorprenden:

1. **La auto-invocación no pasa por el proxy.** Si `subscribe()` llama a `this.cancel()` en el mismo bean, la llamada interna se ejecuta *dentro* de la transacción externa y un `@Transactional(REQUIRES_NEW)` en `cancel()` se ignora. Pon los métodos transaccionales en beans separados, o acepta la propagación de quien llama.
2. **`@Transactional` es metadato, no comportamiento.** Solo funciona cuando un proxy envuelve la llamada. Las llamadas que entran desde fuera del bean pasan por el proxy; las internas, no.

Usa `@Transactional(readOnly = true)` en los servicios de consulta. Permite al driver saltarse el *dirty checking* y expresa la intención.

### 6.3 ¿De verdad hace falta una interfaz para cada caso de uso?

Para las escrituras, sí, casi siempre:

- el controlador depende del contrato, no de la clase;
- las pruebas pueden sustituirla por un fake;
- ArchUnit puede comprobar que todo servicio implementa un puerto;
- la interfaz es el sitio donde documentar el caso de uso sin ruido de implementación.

Para una **lectura** que devuelve una vista y no tiene reglas, la interfaz muchas veces no aporta nada. Una opción pragmática es un `SubscriptionQueryService` sin interfaz, usado por el controlador. Tu repositorio hace exactamente eso: `CatalogQueryService` es interfaz porque existen dos implementaciones, pero no hay ceremonia alrededor de las consultas triviales. No conviertas "todo método necesita una interfaz" en una religión. Convierte "todo caso de uso tiene un contrato" en un principio.

### 6.4 ¿Un puerto por método o uno por agregado?

Empieza con **puertos por capacidad**: `LoadSubscriptionPort`, `SaveSubscriptionPort`, `ChargePaymentPort`. Expresan lo que el caso de uso necesita.

Fúnselos en un `SubscriptionRepository` cuando:

- el mismo adaptador los implemente siempre,
- ningún caso de uso necesite solo uno,
- y tener un montón de interfaces diminutas esté estorbando la lectura.

Sepáralos cuando un caso de uso solo necesite una parte (segregación de interfaces) o cuando adaptadores distintos implementen capacidades distintas (SQL para leer/guardar, Redis para cachear).

La división de la Parte 2 es un buen valor por defecto. Solo sé consistente: no tengas `LoadSubscriptionPort` en una funcionalidad y `SubscriptionRepository` en otra sin un motivo.

### 6.5 ¿Cómo paso el usuario actual o el tenant?

No leas `SecurityContextHolder` dentro del caso de uso. Eso haría depender la aplicación de Spring Security y escondería una entrada.

Pásalo explícitamente:

```java
public record CancelSubscriptionCommand(UUID subscriptionId, UUID actorId) {
}
```

El controlador extrae el usuario autenticado y lo pone en el comando. Así el caso de uso puede comprobar "¿este actor es el dueño de esta suscripción?" como regla de negocio. Ventaja extra: el mismo caso de uso funciona desde un consumidor de Kafka o desde una CLI sin contexto de seguridad.

### 6.6 ¿Dónde van la validación y los mensajes de error?

Tres tipos de validación, tres casas:

| Tipo | Ejemplo | Dónde |
|---|---|---|
| Sintáctica | `planCode` no está vacío, `id` es un UUID | adaptador, con Jakarta Validation sobre el DTO de petición |
| Semántica / invariante | un periodo debe terminar después de empezar, no se puede cancelar dos veces | dominio, en constructores y métodos |
| Autorización | este usuario no puede cancelar esta suscripción | servicio de aplicación (necesita el agregado cargado y el actor) |

El dominio lanza excepciones tipadas (`SamePlanException`), no errores HTTP. El `@RestControllerAdvice` del adaptador las traduce una sola vez. Nunca lances `ResponseStatusException` desde un caso de uso.

### 6.7 Llamadas externas dentro de una transacción

Esta es la trampa que no se menciona en los artículos de arquitectura. `payments.charge()` es una llamada HTTP a Stripe. Si se ejecuta dentro de `@Transactional`, retienes una conexión y bloqueos de la base de datos durante una ida y vuelta por la red, y un timeout puede dejar la transacción en un estado desconocido.

Hay tres patrones razonables, de menor a mayor robustez:

1. **Cobrar antes de la transacción.** Haz el pago y luego abre una transacción corta para guardar el resultado. Simple, pero una caída entre los dos pierde el registro del pago.
2. **Persistir `PENDING`, cobrar y luego confirmar.** La base de datos siempre conoce la intención, y un trabajo de reconciliación arregla las filas atascadas. Es la opción habitual en producción.
3. **Transactional outbox.** Escribes la intención y una fila de outbox en la misma transacción; un worker realiza el cobro. Lo más robusto y lo que más maquinaria exige.

El ejemplo del §5.5 cobra antes de guardar, que es el patrón 1. Para un barrido de renovaciones, tampoco pongas todo el lote en una sola transacción:

```java
@UseCase
public class RenewSubscriptionService implements RenewSubscriptionUseCase {

    // Un barrido programado no debe mantener una transacción abierta alrededor de
    // N llamadas HTTP; cada suscripción se confirma en su propia transacción corta.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void renew(RenewSubscriptionCommand command) {
        Subscription subscription = this.subscriptions.byId(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

        PaymentResult payment = this.payments.charge(new PaymentRequest(
                subscription.id(), subscription.userId(), subscription.amountDue()));

        this.confirmations.confirm(subscription.id(), payment.successful());
    }
}
```

`confirmations` es un bean aparte con `@Transactional`, precisamente por la regla de auto-invocación del §6.2.

### 6.8 ¿Los puertos devuelven objetos de dominio o DTOs?

Los puertos de salida devuelven **objetos de dominio** (o primitivos): `Optional<Subscription>`. Son la vista de la aplicación sobre el exterior, y la aplicación trabaja con el modelo.

Los puertos de entrada devuelven **vistas** (`SubscriptionView`) o nada. Son la API de la aplicación, y el exterior no debe quedar atado a los internos del agregado.

La asimetría es deliberada: dentro, objetos ricos; fuera, contratos estables.

### 6.9 ¿Dónde encajan los eventos de dominio?

Mantén el `ApplicationEventPublisher` de Spring fuera del dominio. Hay dos opciones limpias:

1. **El agregado recoge los eventos**, y el servicio de aplicación los publica tras guardar, a través de un puerto de salida:

```java
public interface DomainEventPublisher {
    void publish(Object event);
}
```

2. **El agregado devuelve el evento** del método que lo produjo, y el servicio decide.

Después, el listener es un **adaptador de entrada**: recibe el evento y llama a un caso de uso. Si escribe en la base de datos, usa `@TransactionalEventListener(phase = AFTER_COMMIT)` para no reaccionar a una transacción que luego va a hacer *rollback*.

Tu repositorio ya tiene `DomainEventPublisher` en `domain/port` y una implementación en `infrastructure/event`. Esa es la forma correcta.

### 6.10 ¿Cómo hablan dos funcionalidades entre sí?

Nunca a través del adaptador o del repositorio de la otra funcionalidad. A través del **puerto de entrada** de la otra:

```
order.application.CheckoutService
    → catalog.application.port.in.ReserveStockUseCase   ✅
    → catalog.infrastructure.persistence.JpaCatalogRepositoryAdapter   ❌
```

Tu regla 3 de ArchUnit (`featuresDoNotCrossIntoOtherInfrastructure`) impone exactamente esto. Si dos funcionalidades necesitan compartir un modelo, la señal es que son un mismo contexto delimitado, o que necesitas un contrato compartido explícito.

### 6.11 ¿Y las tareas programadas y Kafka?

Las dos son **adaptadores de entrada**. Una tarea cron es un adaptador que llama a un caso de uso:

```java
@Component
class RenewalScheduler {

    private final RenewSubscriptionUseCase renewals;

    @Scheduled(cron = "0 0 2 * * *")
    void renewDueSubscriptions() {
        this.renewals.renewDue();
    }
}
```

Un consumidor de Kafka tiene la misma forma: deserializar, validar, construir un comando, llamar al caso de uso. No debe contener reglas de negocio ni tocar un repositorio. Por eso "adaptador de entrada" es un nombre mejor que "controlador": HTTP es solo una de las entradas.

### 6.12 ¿No es sobreingeniería?

A veces, sí. Seamos honestos con las señales.

**Probablemente no necesitas el hexágono completo si:**

- la app es un CRUD fino sobre una tabla;
- hay una entrada y una base de datos, y no planes de cambiar ninguna;
- el "dominio" es un conjunto de campos sin reglas;
- el equipo es una persona y la app se reescribirá en un año.

**Probablemente sí lo necesitas si:**

- las mismas reglas se alcanzan desde HTTP, una cola y un cron;
- hay proveedores externos (pagos, correo, impuestos, envíos) que pueden cambiar;
- las reglas de negocio son la parte valiosa y probada;
- las pruebas son lentas y frágiles porque todo necesita una base de datos;
- más de un equipo toca el código.

Un camino intermedio que funciona bien: mantén el dominio puro y los puertos explícitos, pero prescinde de la interfaz en las lecturas triviales y prescinde del `record` de vista cuando el agregado ya es un record limpio. La arquitectura hexagonal es una dirección, no una cuota de archivos.

---

## 7. Migrar una aplicación CRUD existente

No reescribas. Ahoga el código antiguo, segmento a segmento. Cada paso se puede publicar.

**Paso 1 — Escribe los casos de uso.**
Enumera los verbos que expone la app: "crear item", "aplicar descuento", "cancelar suscripción". Esa lista es tu backlog y tu estructura de paquetes objetivo. Sin código todavía.

**Paso 2 — Elige el segmento con más reglas.**
No el más fácil. El segmento con prorrateo, stock o transiciones de estado es donde la arquitectura se amortiza y donde el código viejo más duele.

**Paso 3 — Construye el dominio, con pruebas, detrás del código viejo.**
Crea `domain/model` para ese segmento. Copia las reglas fuera del servicio o de la entidad. Escribe pruebas JUnit normales. No lo cables todavía. Este es el único paso que es solo adición.

**Paso 4 — Define los puertos de salida que necesita el segmento.**
`Load...Port`, `Save...Port`. Implémentalos delegando en el **repositorio existente**. Ya tienes una costura sin reescribir nada.

**Paso 5 — Define el puerto de entrada y su servicio.**
El servicio orquesta: cargar → método de dominio → guardar. Añade `@Transactional` aquí.

**Paso 6 — Apunta un adaptador al nuevo caso de uso.**
El controlador de ese segmento llama al caso de uso en lugar del servicio viejo. Todo lo demás sigue funcionando. Publícalo.

**Paso 7 — Añade ArchUnit.**
Ahora que un segmento está limpio, las reglas pueden vigilarlo. Usa `allowEmptyShould(true)` para los paquetes aún no migrados, y quítalo a medida que lleguen.

**Paso 8 — Repite. Borra el camino viejo tras cada segmento.**
El último commit de un segmento debe eliminar el código que sustituyó. Si no, acabas con dos arquitecturas y sin forma de saber cuál manda.

**Paso 9 — Solo entonces considera multi-módulo.**
Si ArchUnit basta para impedir violaciones, mantén un solo módulo. Divídelo cuando el cumplimiento en compilación o el despliegue independiente justifiquen la complejidad de build.

Un ritmo realista: un segmento cada una o dos semanas para un equipo pequeño. El primero es el más lento porque además de construir el código estás fijando las convenciones; los siguientes son sobre todo copiar y adaptar.

---

## 8. Señales de alarma y antipatrones

| Señal | Qué significa | Solución |
|---|---|---|
| `@Entity` en una clase de dominio | El dominio es la base de datos | Separa el modelo de dominio de la entidad JPA y añade un mapper |
| El controlador inyecta un repositorio | El adaptador de entrada se salta el caso de uso | Que llame al caso de uso; el repositorio va detrás de un puerto |
| `@Transactional` en un controlador | HTTP es la frontera transaccional | Muévelo al servicio de aplicación |
| Un caso de uso devuelve una entidad JPA | La persistencia se filtra hacia fuera | Mapea a un `record` de vista |
| Un puerto recibe un DTO `...Request` | El lenguaje del adaptador se ha filtrado hacia dentro | Conviértelo en un comando en el adaptador |
| El dominio importa `org.springframework` | El hexágono tiene un agujero | Quita la anotación; cablea desde fuera |
| `if (status == ...)` en un controlador | Regla de negocio en el adaptador | Muévela al agregado |
| Un servicio de aplicación de 500 líneas | Orquestación y reglas mezcladas | Extrae métodos de dominio; divide el caso de uso |
| Un adaptador por método, 30 clases diminutas | Explosión de puertos | Fusiona por capacidad |
| `@SpringBootTest` para probar un precio | El dominio no está aislado | Prueba unitaria normal, sin contexto |
| La misma regla implementada dos veces | Dos fuentes de verdad | Un método de dominio, llamado desde los dos casos de uso |
| `catch (Exception)` en un adaptador y `return null` | Los fallos se convierten en silencio | Devuelve un resultado tipado o lanza una excepción tipada |

Y la lista positiva, para una revisión de código:

- [ ] Cada caso de uso es una interfaz en `application/port/in`, implementada por un servicio.
- [ ] Cada dependencia de salida es una interfaz en `application/port/out`.
- [ ] El dominio solo importa `java.*`.
- [ ] Ningún tipo de adaptador aparece en la firma de la aplicación.
- [ ] `@Transactional` está en el servicio de aplicación.
- [ ] El controlador solo mapea DTO → comando y vista → respuesta, nada más.
- [ ] El adaptador de persistencia mapea entidad ↔ dominio y es dueño de las anotaciones JPA.
- [ ] Cada proveedor externo tiene su propio puerto.
- [ ] Las pruebas de dominio no necesitan Spring; las de caso de uso usan fakes.
- [ ] ArchUnit pasa, y al menos una regla se ha demostrado capaz de fallar a propósito.

---

## 9. Chuleta

**Las tres palabras**

| Palabra | Pregunta que responde | Vive en | Ejemplo |
|---|---|---|---|
| Caso de uso | ¿Qué me puede pedir el exterior? | `application/port/in` | `CancelSubscriptionUseCase` |
| Puerto | ¿Qué necesito / qué ofrezco? | `application/port/{in,out}` | `SaveSubscriptionPort` |
| Adaptador | ¿Cómo lo hace una tecnología concreta? | `adapter/{in,out}` | `SubscriptionPersistenceAdapter` |

**La regla de dependencia**

```
adapter → application → domain
adapter → domain
domain  → nada
```

**El recorrido de una petición**

```
HTTP → Controlador → CasoDeUso (interfaz) → Servicio → método de dominio
                                             → Puerto (interfaz) → Adaptador → BD / Stripe
```

**Dónde va cada cosa**

| Si estás escribiendo... | Va en... |
|---|---|
| Un cálculo sobre dos objetos de valor | `domain/service` |
| Una transición de estado | el agregado, `domain/model` |
| Un controlador, consumidor, tarea programada | `adapter/in` |
| Una entidad JPA, un repositorio, un mapper | `adapter/out/persistence` |
| Un wrapper de un SDK | `adapter/out/<proveedor>` |
| Un comando o una vista | `application/command`, `application/view` |
| La unión de un puerto con su adaptador | `config` |
| La traducción de excepciones a códigos HTTP | `adapter/in/web` |

**Las cinco reglas para recordar**

1. El dominio es Java puro. Si importa Spring, no es el dominio.
2. Las interfaces son del que llama. El puerto vive con el caso de uso; el adaptador, fuera.
3. El controlador depende de la interfaz del caso de uso, nunca de la clase de servicio.
4. `@Transactional` envuelve el caso de uso, ni el controlador ni la entidad.
5. El modelo de base de datos y el modelo de dominio son modelos distintos. El mapper es el precio de la libertad.

**Un párrafo para llevar contigo**

La arquitectura hexagonal no es un árbol de carpetas. Es una única promesa: *las reglas de negocio no saben cómo se entregan ni cómo se guardan*. Los puertos son la forma de mantener esa promesa a nivel de tipos; los adaptadores son donde vive la tecnología; y los casos de uso son las unidades de trabajo que conectan ambos. Si puedes ejecutar las pruebas de tu dominio sin Spring, sustituir tu adaptador de base de datos por un fake en memoria y añadir un consumidor de Kafka sin tocar una regla de negocio, tienes la arquitectura, sin importar cómo se llamen los paquetes.

---

## 10. Lecturas recomendadas

- **La serie:** [Parte 1 — por qué hexagonal](https://vitelco.com/vitelco-blog/hexagonal-architecture-with-spring-boot), [Parte 2 — puertos, adaptadores y casos de uso](https://vitelco.com/vitelco-blog/hexagonal-architecture-with-spring-boot-part-2). La Parte 3 empieza por la capa de dominio, que es el §5.2 de esta guía.
- **Alistair Cockburn**, *Hexagonal Architecture (Ports and Adapters)* — el artículo original. Corto, y sigue siendo la exposición más clara de la intención.
- **Tom Hombergs**, *Get Your Hands Dirty on Clean Architecture* — la versión en libro de esta guía, con mucho énfasis en la regla de dependencia y en las pruebas.
- **Vaughn Vernon**, *Implementing Domain-Driven Design* — para agregados, objetos de valor y eventos de dominio, que el ejemplo de suscripción solo esboza.
- **Guía de ArchUnit** — para las reglas que impiden que todo lo anterior se degrade.
- **Este repositorio** — `backend/src/main/java/com/anderk/ecommerce`: una implementación real y probada de las mismas ideas en `catalog`, `lead`, `order`, `audit` y `auth`, con `@UseCase`, cableado explícito en `@Configuration` y `ArchitectureTest`. Es la mejor referencia local de las convenciones del §2.6 y del §5.9.
