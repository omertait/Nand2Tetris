// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
//
// This program only needs to handle arguments that satisfy
// R0 >= 0, R1 >= 0, and R0*R1 < 32768.

// Put your code here.
// SET R2 to 0
@R2
M=0
// IF R0 = 0 -> CONTINUE, ELSE -> END
@R0
D=M
@END
D;JEQ
// IF R1 > 0 -> CONTINUE, ELSE -> END
@R1
D=M
@END
D;JEQ
//ELSE
(LOOP)
    // REDUCE 1 FROM R1
    @R1
    M=M-1
    // ADD R0 TO R@
    @R0
    D=M
    @R2
    M=M+D
    // IF R1 > 0 -> REPEAT, ELSE -> DONE
    @R1
    D=M
    @LOOP
    D;JGT
(END)
    @END
    0;JMP


