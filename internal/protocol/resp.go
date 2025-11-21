package protocol

import (
	"bufio"
	"fmt"
	"io"
	"strconv"
	"strings"
)

// RESP protocol types
const (
	SimpleString = '+'
	Error        = '-'
	Integer      = ':'
	BulkString   = '$'
	Array        = '*'
)

type RESPReader struct {
	reader *bufio.Reader
}

func NewRESPReader(r io.Reader) *RESPReader {
	return &RESPReader{
		reader: bufio.NewReader(r),
	}
}

// ReadArray reads a RESP array and returns the elements
func (r *RESPReader) ReadArray() ([]string, error) {
	// Read the first byte to determine type
	typeByte, err := r.reader.ReadByte()
	if err != nil {
		return nil, err
	}

	if typeByte != Array {
		return nil, fmt.Errorf("expected array, got %c", typeByte)
	}

	// Read array length
	line, err := r.reader.ReadString('\n')
	if err != nil {
		return nil, err
	}
	line = strings.TrimSpace(line)

	count, err := strconv.Atoi(line)
	if err != nil {
		return nil, fmt.Errorf("invalid array length: %s", line)
	}

	// Read each element
	elements := make([]string, count)
	for i := 0; i < count; i++ {
		elem, err := r.readBulkString()
		if err != nil {
			return nil, err
		}
		elements[i] = elem
	}

	return elements, nil
}

// readBulkString reads a RESP bulk string
func (r *RESPReader) readBulkString() (string, error) {
	// Read the type byte
	typeByte, err := r.reader.ReadByte()
	if err != nil {
		return "", err
	}

	if typeByte != BulkString {
		return "", fmt.Errorf("expected bulk string, got %c", typeByte)
	}

	// Read the length
	line, err := r.reader.ReadString('\n')
	if err != nil {
		return "", err
	}
	line = strings.TrimSpace(line)

	length, err := strconv.Atoi(line)
	if err != nil {
		return "", fmt.Errorf("invalid bulk string length: %s", line)
	}

	if length == -1 {
		return "", nil // Null bulk string
	}

	// Read the actual string data
	data := make([]byte, length+2) // +2 for \r\n
	_, err = io.ReadFull(r.reader, data)
	if err != nil {
		return "", err
	}

	return string(data[:length]), nil
}

// WriteSimpleString writes a RESP simple string
func WriteSimpleString(w io.Writer, s string) error {
	_, err := fmt.Fprintf(w, "+%s\r\n", s)
	return err
}

// WriteBulkString writes a RESP bulk string
func WriteBulkString(w io.Writer, s string) error {
	_, err := fmt.Fprintf(w, "$%d\r\n%s\r\n", len(s), s)
	return err
}

// WriteError writes a RESP error
func WriteError(w io.Writer, msg string) error {
	_, err := fmt.Fprintf(w, "-%s\r\n", msg)
	return err
}

// WriteNull writes a RESP null bulk string
func WriteNull(w io.Writer) error {
	_, err := fmt.Fprintf(w, "$-1\r\n")
	return err
}
