#include <signal.h>
#include <unistd.h>
#include <stdio.h>

int main(int argc, char *argv[])
{
  struct sigaction sa = { 0 };

  sa.sa_handler = SIG_IGN;
  sigaction(SIGTERM, &sa, 0);

  if (argc > 1) {
    execvp(argv[1], argv + 1);
    perror("execv");
  } else {
    fprintf(stderr, "Usage: %s <command> [args...]\n", argv[0]);
  }
  return 1;
}
