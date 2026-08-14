// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Fill.asm

// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, 
// the screen should be cleared.

// SCREEN = 256 rows x 512 pixels (32 x 16-bit words)  @ 0x4000
// KBD = 16 bits @ 0x6000
// 0 = white, 1 = black

// R0 = color
// R1 = rows
// R2 = cols
// R3 = index into SCREEN

(LOOP)
    @KBD
    D=M
    @FILLWHITE
    D;JEQ
    @FILLBLACK
    0;JMP
(FILLWHITE)
    @R0
    D=M
    @0
    D=D-A
    // Short circuit if we're already in the desired state.
    @LOOP
    D;JEQ

    @R0
    M=0
    @FILL
    0;JMP
(FILLBLACK)
    @R0
    D=M
    A=-1
    D=D-A
    @LOOP
    D;JEQ

    @R0
    M=-1
    @FILL
    0;JMP
(FILL)
    // Init variables for filling the screen
    @R1
    M=0
    @R2
    M=0
    @SCREEN
    D=A
    @R3
    M=D

    // Start in the inner loop
    @INNER
    0;JMP
(INNER)
    @R0
    D=M
    @R3
    A=M
    M=D
    
    // Bump to next pixel
    @R3
    M=M+1

    // Keep looping over columns or finish?
    @R2
    MD=M+1
    @32
    D=A-D
    @OUTER
    D;JEQ
    @INNER
    0;JMP
(OUTER)
    // Bump row index
    @R1
    MD=M+1
    @256
    D=A-D

    @LOOP
    D;JEQ

    @R2
    M=0
    @INNER
    0;JMP
