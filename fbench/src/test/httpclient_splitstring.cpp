// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <httpclient/httpclient.h>
#include <cstring>

class DebugHTTPClient : public HTTPClient
{
public:
  DebugHTTPClient(const char* server, int port, bool keepAlive)
    : HTTPClient(server, port, keepAlive, true) {}

  static void SplitLineTest(const char *input);
  static void DebugSplitLine();
};

void
DebugHTTPClient::SplitLineTest(const char *input)
{
  char        str[1024];
  char       *rest;
  int         argc;
  char       *argv[5];
  int         i;

  memcpy(str, input, strlen(input) + 1);
  printf("*** TEST HTTPClient::SplitString ***\n");
  printf("string:'%s'\n", str);
  rest = str;
  while (rest != NULL) {
    rest = SplitString(rest, argc, argv, 5);
    printf("argc:'%d'\n", argc);
    printf("rest:'%s'\n", (rest == NULL) ? "NULL" : rest);
    for(i = 0; i < argc; i++) {
      printf("  %d:'%s'\n", i, argv[i]);
    }
  }
}

void
DebugHTTPClient::DebugSplitLine()
{
  SplitLineTest("This is a test");
  SplitLineTest("This is exactly five words");
  SplitLineTest("five words with traling space ");
  SplitLineTest(" This\t is \ta \t harder\ttest  ");
  SplitLineTest("SingleWord");
  SplitLineTest("\t\t  \t\tSingleWordWithSpacesAround  \t\t  ");
  SplitLineTest("just all too many parts  baby ");
  SplitLineTest("many many words does this long fancy string contain "
		", and they all must be tokenized by split line");
}

int
main(int argc, char **argv)
{
  (void) argc;
  (void) argv;
  DebugHTTPClient::DebugSplitLine();
}
