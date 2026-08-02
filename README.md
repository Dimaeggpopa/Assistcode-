# SmartKey AI

Android-приложение с кастомной AI-клавиатурой (аналог CleverType).
Состоит из двух частей:

- **Настройки/онбординг** (`MainActivity`) — включение клавиатуры, ввод собственного Anthropic API-ключа
- **Сама клавиатура** (`AIKeyboardService`) — классическая QWERTY-раскладка + панель AI-действий (Переписать / Грамматика / Короче / Дружелюбнее)

Код на чистой Java, без Kotlin — специально под сборку в Termux без Android Studio.

## Как это работает

1. Пользователь вводит **свой** Anthropic API-ключ в настройках приложения (хранится зашифрованным через `EncryptedSharedPreferences`, никуда кроме api.anthropic.com не уходит)
2. Клавиатура включается через системные настройки ввода
3. Кнопки AI-панели забирают текущий текст из поля ввода, отправляют инструкцию в Claude API и заменяют текст результатом

## Сборка в Termux

### 1. Базовые пакеты

```bash
pkg update && pkg upgrade -y
pkg install -y git openjdk-17 wget unzip
```

### 2. Android SDK (командная строка, без Android Studio)

```bash
cd ~
mkdir -p android-sdk/cmdline-tools
cd android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest
```

Пропиши переменные окружения (добавь в `~/.bashrc`, потом `source ~/.bashrc`):

```bash
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

Установи нужные компоненты и прими лицензии:

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

> Проверь актуальную ссылку на cmdline-tools на странице
> https://developer.android.com/studio#command-line-tools-only — версия в архиве периодически меняется.

### 3. Gradle

```bash
pkg install -y gradle
gradle -v
```

Если версия Gradle из `pkg` старше 8.7 (нужна для AGP 8.5.2) — либо обнови пакет, либо понизь версию AGP в `build.gradle.kts` под то, что есть.

### 4. Сборка APK

Из корня проекта:

```bash
cd ai-keyboard-android
gradle assembleDebug
```

Готовый APK окажется в:

```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Установка на телефон

Если Termux работает прямо на целевом устройстве:

```bash
pkg install -y android-tools
# включи "Отладка по USB" или используй установку через файловый менеджер
cp app/build/outputs/apk/debug/app-debug.apk ~/storage/downloads/
```

Затем открой файл через файловый менеджер и установи (разреши установку из неизвестных источников).

## Публикация на GitHub

```bash
cd ai-keyboard-android
git init
git add .
git commit -m "Initial commit: SmartKey AI keyboard skeleton"

# создай пустой репозиторий на github.com, затем:
git branch -M main
git remote add origin https://github.com/<твой-юзернейм>/smartkey-ai.git
git push -u origin main
```

`.gitignore` уже настроен так, чтобы не коммитить `build/`, `.gradle/`, `local.properties` и сами `.apk`.

## Что уже сделано (MVP)

- [x] Базовый QWERTY-ввод (буквы, shift, backspace, пробел, точка/запятая, enter)
- [x] Панель AI-действий с 4 функциями
- [x] Безопасное хранение API-ключа на устройстве
- [x] Работа с выделенным текстом или всем текстом поля

## Что стоит доделать дальше

- [ ] Русская раскладка (сейчас только английская QWERTY — `keyboard_qwerty.xml`)
- [ ] Экранная клавиатура символов/цифр (`?123`) — сейчас код есть, раскладки нет
- [ ] Свайп-ввод / предиктивный набор
- [ ] Индикатор "AI думает" прямо во всплывающей подсказке, а не только на панели
- [ ] Кэширование последнего результата, чтобы можно было откатить замену
- [ ] Проверка, чтобы AI-кнопки не били по лимитам API слишком часто (debounce)
- [ ] Собственный бэкенд-прокси вместо прямого вызова с телефона (чтобы не хранить ключ на устройстве вообще)

Скажи, что делать дальше — добавляю русскую раскладку, экран символов, или сразу двигаемся в сторону iOS-версии.
