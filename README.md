# Minhas chaves Pix

Aplicativo Android para guardar e consultar chaves Pix organizadas por beneficiário e banco. Os dados ficam no armazenamento local do dispositivo; o app não solicita permissão de Internet.

## Funcionalidades

- Cadastros de beneficiários e bancos, com até cinco chaves Pix por cadastro.
- Consulta, cópia e geração de QR Code Pix estático, com valor opcional.
- Compartilhamento do QR Code como imagem.
- Exportação e importação manual de backups JSON.
- Temas claro, escuro e baseado no sistema.

Os backups JSON são exportados sem criptografia. Guarde-os em local seguro e compartilhe-os somente com pessoas de confiança.

## Requisitos

- Android Studio Jellyfish ou mais recente.
- JDK 17.
- Android SDK 34 para compilar; o app oferece suporte a partir do Android 6.0 (API 23).

## Compilar e testar

Na raiz do projeto:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/`.

## Estrutura

- `app/src/main/java/`: telas, armazenamento local, validação, formatação e geração de payload/QR.
- `app/src/main/res/`: layouts, temas, textos e recursos visuais.
- `app/src/test/java/`: testes unitários de regras, migração, backup, chaves e payload Pix.

## Privacidade e aviso

As chaves são armazenadas no dispositivo e o app não tem permissão de Internet. A exportação de backup e o compartilhamento de QR são ações iniciadas pelo usuário e usam os mecanismos do Android.

Este aplicativo não é oficial nem afiliado a qualquer banco ou ao Banco Central. Confira os dados do beneficiário e o valor antes de confirmar um pagamento.

> Ao preparar capturas de tela para o repositório, use somente dados inteiramente fictícios.
