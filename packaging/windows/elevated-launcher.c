#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <shellapi.h>
#include <wchar.h>

int WINAPI wWinMain(HINSTANCE instance, HINSTANCE previous, PWSTR command_line, int show_command) {
    wchar_t launcher_path[MAX_PATH];
    wchar_t application_path[MAX_PATH];
    wchar_t *separator;
    SHELLEXECUTEINFOW execution;
    DWORD length;
    (void)instance;
    (void)previous;
    (void)command_line;
    (void)show_command;

    length = GetModuleFileNameW(NULL, launcher_path, MAX_PATH);
    if (length == 0 || length >= MAX_PATH) {
        MessageBoxW(NULL, L"Could not determine the application directory.",
                    L"Network User Registration", MB_OK | MB_ICONERROR);
        return 1;
    }
    separator = wcsrchr(launcher_path, L'\\');
    if (separator == NULL) return 1;
    *separator = L'\0';
    if (_snwprintf(application_path, MAX_PATH, L"%ls\\NetworkUserRegistrationApp.exe",
                   launcher_path) < 0) return 1;

    SetCurrentDirectoryW(launcher_path);
    ZeroMemory(&execution, sizeof(execution));
    execution.cbSize = sizeof(execution);
    execution.fMask = SEE_MASK_NOCLOSEPROCESS;
    execution.lpFile = application_path;
    execution.lpDirectory = launcher_path;
    execution.nShow = SW_SHOWNORMAL;
    if (!ShellExecuteExW(&execution)) {
        wchar_t message[256];
        _snwprintf(message, 256, L"Could not start the packaged application (Windows error %lu).",
                   GetLastError());
        MessageBoxW(NULL, message, L"Network User Registration", MB_OK | MB_ICONERROR);
        return 1;
    }
    if (execution.hProcess != NULL) CloseHandle(execution.hProcess);
    return 0;
}
