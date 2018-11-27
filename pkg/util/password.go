/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package util

import (
	"crypto/rand"
	"math/big"
)

var (
	PossibleCharacters = []rune("abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + ";:_,.-#+*=?()/&%$!")
	MaxLen             = big.NewInt(int64(len(PossibleCharacters)))
)

func randomChar() (rune, error) {

	val, err := rand.Int(rand.Reader, MaxLen)
	if err != nil {
		return '\000', err
	}

	return PossibleCharacters[val.Int64()], nil
}

func GeneratePassword(length int) (string, error) {
	result := make([]rune, length)

	for i := 0; i < length; i++ {
		r, err := randomChar()
		if err != nil {
			return "", err
		}
		result[i] = r
	}

	return string(result), nil
}
