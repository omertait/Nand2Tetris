// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed. 
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
    //INIT
    @SCREEN
    D=A 
    @ADDR
    M=D

    (WHITE)
    // CHECK KEYBOARD INPUT
        @KBD 
        D=M 
        @BLACK
        D;JGT
    // if ADDR = SCREEN -> LOOP UNTIL PRESSED A KEY (ALL SCREEN IS WHITE)
        @ADDR
        D=M
        @SCREEN
        D=D-A
        @WHITE
        D;JEQ
    // ADDR-- (WHITE COLOR JUST THE BLACK PIXELS)
        @ADDR
        M=M-1
    // GO TO RAM[ADDR]
        @ADDR
        A=M
    // FILL WHITE RAM[ADDR]
        M=0
    // LOOP AND CHECK AGAIN FOR INPUT
        @WHITE 
        0;JMP
   

    (BLACK)
    // CHECK KEYBOARD INPUT
        @KBD 
        D=M 
        @WHITE
        D;JEQ
    // IF CURRENT ADDR = KBD (ALL SCREEN IS COLORED) -> WHITE (LOOP UNTIL KEY CHANGES)
        @ADDR
        D=M
        @KBD
        D=D-A 
        @WHITE
        D;JEQ
    // GO TO RAM[ADDR]
        @ADDR
        A=M
    // FILL BLACK RAM[ADDR]
        M=-1
    // ADDR++ (COLOR THE NEXT PIXELS)
        @ADDR
        M=M+1
    // LOOP AND CHECK AGAIN FOR INPUT
        @BLACK 
        0;JMP


   
    
