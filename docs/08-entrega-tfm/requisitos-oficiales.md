# Requisitos oficiales del TFM

Este documento transcribe los requisitos académicos oficiales aplicables a la entrega de LLM Scanner Seguridad. No define funcionalidades del producto ni decisiones técnicas.

## Objetivo del TFM

El proyecto debe ser:

- Una aplicación real.
- Adaptada al nivel de conocimientos y experiencia del alumno.
- Original.
- Capaz de demostrar los conocimientos adquiridos durante el máster.
- Diferenciadora.
- Con valor personal y profesional.

No existe una tecnología obligatoria.

## README obligatorio

La entrega debe disponer de un `README.md` completo y detallado en la raíz del repositorio. Debe poder explicar el proyecto por sí mismo e incluir, como mínimo:

1. Descripción general del proyecto.
2. Stack tecnológico utilizado.
3. Información de instalación.
4. Información de ejecución.
5. Estructura del proyecto.
6. Funcionalidades principales.
7. Usuario y contraseña de prueba, si existe login.

La documentación de `docs/` puede ampliar esta información, pero no sustituye el contenido obligatorio del README raíz.

## Código fuente

El código debe entregarse mediante GitHub.

Opciones:

- Repositorio público.
- Repositorio privado únicamente si existe justificación.

Si se utiliza un repositorio privado, deberá gestionarse el acceso requerido por la organización del máster. No deben añadirse emails personales ni credenciales en archivos públicos si no es necesario.

## Despliegue

Debe intentarse disponer de un despliegue o publicación funcional siempre que sea posible.

La documentación final debe incluir la URL real del proyecto desplegado. Para LLM Scanner Seguridad está previsto un despliegue final en AWS.

Estado actual: **COMPLETADO para publicación funcional observada por el operador el 2026-10-20**;
URL en [enlaces-entrega.md](enlaces-entrega.md) y [evidencia AWS](../../openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md).
Preparación técnica del acceso académico completada por review (6.4). Las credenciales se
facilitarán por canal privado. D completada el 2026-10-23: TLS/401 comprobados directamente
y UI autenticada con proyectos confirmada por el operador. Entrega efectiva y confirmación
administrativa pendientes de H; no se declaran realizadas por el cierre técnico.
24/25 tareas completas: 6.1 completada tras confirmación operativa/TLS del operador;
solo 6.3 parcial por restore aislado expresamente pospuesto, no bloqueante para la entrega
académica. Esta actualización de estado no modifica el requisito oficial.

No se registrará ninguna URL hasta que exista un despliegue real.

## Presentación

La entrega requiere slides de presentación del proyecto. Podrán realizarse con Google Slides, PowerPoint, Canva u otra herramienta equivalente.

La entrega deberá disponer de una URL pública o del documento correspondiente, y el enlace deberá aparecer en la documentación del proyecto.

Estado actual: **COMPLETADO — Fase E, 2026-10-24**. Presentación final de 10 slides;
PDF y PPTX revisados y conservados en `slides/`. [URL oficial y acceso](enlaces-entrega.md#slides):
Drive configurado como lector para cualquiera con enlace según operador y visor abierto
sin login obligatorio ni solicitud de permisos. Esta actualización de estado no modifica
la obligación oficial ni afirma vídeo, formulario o entrega de credenciales completados.

## Vídeo

La entrega requiere un vídeo en el que el alumno:

- Explique personalmente el proyecto.
- Muestre una captura o grabación de pantalla durante la explicación.

La grabación del rostro es opcional. La captura de pantalla durante la explicación es obligatoria.

El vídeo podrá publicarse mediante YouTube, Drive u otra plataforma equivalente. Su URL pública deberá aparecer en la documentación.

Estado actual: **PENDIENTE**.

El vídeo no se crea en esta fase.

## Contenido del directorio del proyecto

El repositorio o directorio del código debe contener:

- Documentación.
- Información sobre el despliegue.
- Las slides o un enlace hacia las slides.

La carpeta `docs/08-entrega-tfm/` permite controlar progresivamente estos requisitos sin incorporarlos al dominio del producto.

## Formulario final de entrega

El formulario final requerirá disponer de:

- Nombre completo del alumno.
- Email utilizado en la inscripción.
- URL del repositorio GitHub.
- URL del despliegue, si existe.
- URL de las slides.
- URL del vídeo.
- Usuario y contraseña de prueba, si existe login.

Los datos personales, URLs y credenciales se rellenarán únicamente cuando correspondan. No se almacenan en este documento.

## Separación entre producto y entrega académica

Los requisitos de vídeo, slides, formulario y publicación no forman parte del dominio de LLM Scanner Seguridad ni deben convertirse en capabilities de OpenSpec. Son requisitos de la entrega académica.

