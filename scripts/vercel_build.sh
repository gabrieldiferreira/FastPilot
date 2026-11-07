#!/usr/bin/env bash
# Linha acima (shebang): faz o sistema executar este arquivo com o bash.
# Vercel build for the Web (wasmJs) target. The Vercel build image has Node but no JDK,
# so a JDK 17 is downloaded first. Variables come from the project's environment (.env).
# The Android SDK is not needed: the wasmJs tasks configure and build without it.
# Resumo: script de build da versão Web na Vercel; baixa um JDK 17 (a Vercel não tem Java) e roda o Gradle.
# Faz o script parar no primeiro erro (-e), tratar variável não definida como erro (-u) e falhar se qualquer parte de um pipe falhar (-o pipefail).
set -euo pipefail

# Define o endereço de download do JDK 17, a menos que a variável JDK_URL já venha do ambiente.
: "${JDK_URL:=https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse}"
# Define a tarefa do Gradle que gera a versão Web, a menos que GRADLE_TASK já venha do ambiente.
: "${GRADLE_TASK:=:composeApp:wasmJsBrowserDistribution}"
# Define as opções de memória da JVM do Gradle, a menos que GRADLE_JVMARGS já venha do ambiente.
: "${GRADLE_JVMARGS:=-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8}"

# Pasta onde o JDK fica guardado (reaproveitada entre builds); usa JDK_DIR do ambiente se existir.
JDK_DIR="${JDK_DIR:-$HOME/.cache/fastpilot-jdk}"
# Se ainda não existe um executável java nessa pasta...
if [ ! -x "$JDK_DIR/bin/java" ]; then
  # ...avisa no log que vai baixar o JDK...
  echo "Downloading JDK from $JDK_URL"
  # ...cria a pasta (e as pastas acima dela, se faltarem)...
  mkdir -p "$JDK_DIR"
  # ...baixa o arquivo compactado e o descompacta direto na pasta, sem o nível de pasta de cima.
  curl -fsSL "$JDK_URL" | tar -xz --strip-components=1 -C "$JDK_DIR"
# Fim do bloco "se".
fi
# Diz ao Gradle onde está o Java.
export JAVA_HOME="$JDK_DIR"
# Coloca o java desse JDK na frente do PATH, para ser o usado nos comandos.
export PATH="$JAVA_HOME/bin:$PATH"
# Mostra a versão do Java no log, para conferir que o JDK certo foi encontrado.
java -version

# Garante que o wrapper do Gradle tem permissão de execução.
chmod +x ./gradlew
# Roda a tarefa de build da versão Web sem daemon, com rastreio de erro completo e as opções de memória definidas acima.
./gradlew "$GRADLE_TASK" --no-daemon --stacktrace -Dorg.gradle.jvmargs="$GRADLE_JVMARGS"
