// Prog.asm -- sums 1..R0 into R1.
//
// Exercises every feature the assembler has to handle:
//   constants, predefined symbols, variables, forward and backward labels,
//   dest-only / jump-only / full C-instructions, both A and M computations,
//   full-line comments, trailing comments, blank lines and stray whitespace.

    @i
    M=1                // i = 1
    @sum
    M=0                // sum = 0

(LOOP)
    @i
    D=M
    @R0
    D=D-M              // D = i - n
    @STOP
    D;JGT              // forward reference: if i > n, stop

    @sum
    D=M
    @i
    D = D + M          // internal whitespace is legal
    @sum
    M=D                // sum = sum + i

    @i
    M=M+1
    @LOOP
    0;JMP              // backward reference

(STOP)
    @sum
    D=M
    @R1
    M=D                // R1 = sum

(END)
    @END
    0;JMP
