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

(LOOP)
    @SCREEN
    D=A
    @R0
    M=D

    @KBD
    D=M
    @FILLWHITE
    D;JEQ
    @FILLBLACK
    0;JMP

(FILLWHITE)
    @R1
    M=0
    @FILLSCREEN
    0;JMP

(FILLBLACK)
    @R1
    M=-1
    @FILLSCREEN
    0;JMP

(FILLSCREEN)
    // Fill the pixel with the color from R1
    @R1
    D=M
    @R0
    A=M
    M=D

    // Check if all pixels have been processed
    @R0
    D=M+1
    @KBD
    D=A-D
    @LOOP
    D;JEQ

    // Otherwise, keep filling pixels
    @R0
    M=M+1
    @FILLSCREEN
    0;JMP
