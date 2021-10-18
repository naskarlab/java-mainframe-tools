       IDENTIFICATION DIVISION.
       PROGRAM-ID. PROG1.
       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES. DECIMAL-POINT IS COMMA.
       DATA DIVISION.
       WORKING-STORAGE SECTION.

       01  CAMPOS-CICS.
          05 RESP-WS                   PIC S9(8) COMP   VALUE ZEROS.
          05 TAM-WS                    PIC S9(4) COMP   VALUE ZEROS.

       LINKAGE SECTION.

       01  DFHCOMMAREA.
          05 AREA-COMM                 PIC X(1326).
          05 AREA-FILLER               PIC X(0674).

       CONTROLE.
           EXEC CICS RETURN END-EXEC.
