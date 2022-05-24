package com.example.sensorapplication.utils


import java.nio.ByteBuffer
import java.nio.ByteOrder

object TypeConverter {
    /**
     * Convert *four* bytes to an int.
     * @param bytes an array with bytes, of length four or greater
     * @param offset Index of the first byte in the sequence of four.
     * @return The (Java) int corresponding to the four bytes.
     */
    fun fourBytesToInt(bytes: ByteArray?, offset: Int): Int {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
    }

    /**
     * Convert *four* bytes to a float.
     * @param bytes an array with bytes, of length four or greater
     * @param offset Index of the first byte in the sequence of four.
     * @return The (Java) float corresponding to the four bytes.
     */
    fun fourBytesToFloat(bytes: ByteArray?, offset: Int): Float {
        return ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).float
    }

    /**
     * Create a an array of bytes representing a Movesense 2.0 command string, ASCII encoded..
     * The first byte is always set to 1.
     *
     * @param id      The id used to identify this command, and incoming data from sensor.
     * @param command The command, see http://www.movesense.com/docs/esw/api_reference/.
     * @return An array of bytes representing a Movesense 2.0 command string.
     */
    fun stringToAsciiArray(id: Byte, command: String): ByteArray {
        require(id <= 127) { "id= $id" }
        val chars = command.trim { it <= ' ' }.toCharArray()
        val ascii = ByteArray(chars.size + 2)
        ascii[0] = 1
        ascii[1] = id
        for (i in chars.indices) {
            require(chars[i] <= 127.toChar()) {
                "ascii val= " + chars[i]
                    .toInt()
            }
            ascii[i + 2] = chars[i].toByte()
        }
        return ascii
    }

    fun stringToAsciiArray(str: String): ByteArray {
        val chars = str.trim { it <= ' ' }.toCharArray()
        val ascii = ByteArray(chars.size)
        for (i in chars.indices) {
            require(chars[i] <= 127.toChar()) {
                "ascii val= " + chars[i]
                    .toInt()
            }
            ascii[i] = chars[i].toByte()
        }
        return ascii
    }
}