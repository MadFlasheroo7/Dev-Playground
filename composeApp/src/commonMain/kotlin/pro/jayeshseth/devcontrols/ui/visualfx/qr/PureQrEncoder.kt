package pro.jayeshseth.devcontrols.ui.visualfx.qr

/**
 * Pure Kotlin Multiplatform QR Code Generator (ISO/IEC 18004).
 * Supports standard byte mode encoding, Versions 1..10, Error Correction Levels L, M, Q, H,
 * mask penalty evaluation, and provides module metadata for particle simulation.
 */

enum class QrEcLevel(val ordinalBits: Int, val formatBits: Int) {
    L(0, 1), // Low (~7%)
    M(1, 0), // Medium (~15%)
    Q(2, 3), // Quartile (~25%)
    H(3, 2)  // High (~30%)
}

data class QrMatrix(
    val version: Int,
    val size: Int,
    val modules: Array<BooleanArray>
) {
    fun isDark(row: Int, col: Int): Boolean {
        if (row !in 0 until size || col !in 0 until size) return false
        return modules[row][col]
    }

    /**
     * Determines if a cell belongs to one of the 3 corner finder pattern eyes (7x7).
     */
    fun isFinderPattern(row: Int, col: Int): Boolean {
        val inTopLeft = row in 0..6 && col in 0..6
        val inTopRight = row in 0..6 && col in (size - 7) until size
        val inBottomLeft = row in (size - 7) until size && col in 0..6
        return inTopLeft || inTopRight || inBottomLeft
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QrMatrix) return false
        if (version != other.version || size != other.size) return false
        return modules.contentDeepEquals(other.modules)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + size
        result = 31 * result + modules.contentDeepHashCode()
        return result
    }
}

object PureQrEncoder {

    // Galois Field GF(256) tables
    private val expTable = IntArray(512)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            expTable[i + 255] = x
            logTable[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D // Primitive polynomial x^8 + x^4 + x^3 + x^2 + 1
            }
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[logTable[x] + logTable[y]]
    }

    private data class VersionEcBlock(
        val totalCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsGroup1: Int,
        val numBlocksGroup2: Int = 0,
        val dataCodewordsGroup2: Int = 0
    ) {
        val totalDataCodewords: Int
            get() = numBlocksGroup1 * dataCodewordsGroup1 + numBlocksGroup2 * dataCodewordsGroup2
    }

    // Version definitions (1..10) for EC Levels: L, M, Q, H
    private val EC_TABLE = arrayOf(
        // V1
        arrayOf(
            VersionEcBlock(26, 7, 1, 19),
            VersionEcBlock(26, 10, 1, 16),
            VersionEcBlock(26, 13, 1, 13),
            VersionEcBlock(26, 17, 1, 9)
        ),
        // V2
        arrayOf(
            VersionEcBlock(44, 10, 1, 34),
            VersionEcBlock(44, 16, 1, 28),
            VersionEcBlock(44, 22, 1, 22),
            VersionEcBlock(44, 28, 1, 16)
        ),
        // V3
        arrayOf(
            VersionEcBlock(70, 15, 1, 55),
            VersionEcBlock(70, 26, 1, 44),
            VersionEcBlock(70, 18, 2, 17),
            VersionEcBlock(70, 22, 2, 13)
        ),
        // V4
        arrayOf(
            VersionEcBlock(100, 20, 1, 80),
            VersionEcBlock(100, 18, 2, 32),
            VersionEcBlock(100, 26, 2, 24),
            VersionEcBlock(100, 16, 4, 9)
        ),
        // V5
        arrayOf(
            VersionEcBlock(134, 26, 1, 108),
            VersionEcBlock(134, 24, 2, 43),
            VersionEcBlock(134, 18, 2, 15, 2, 16),
            VersionEcBlock(134, 22, 2, 11, 2, 12)
        ),
        // V6
        arrayOf(
            VersionEcBlock(172, 18, 2, 68),
            VersionEcBlock(172, 16, 4, 27),
            VersionEcBlock(172, 24, 4, 19),
            VersionEcBlock(172, 28, 4, 15)
        ),
        // V7
        arrayOf(
            VersionEcBlock(196, 20, 2, 78),
            VersionEcBlock(196, 18, 4, 31),
            VersionEcBlock(196, 18, 2, 14, 4, 15),
            VersionEcBlock(196, 26, 4, 13, 1, 14)
        ),
        // V8
        arrayOf(
            VersionEcBlock(242, 24, 2, 97),
            VersionEcBlock(242, 22, 2, 38, 2, 39),
            VersionEcBlock(242, 22, 4, 18, 2, 19),
            VersionEcBlock(242, 26, 4, 14, 2, 15)
        ),
        // V9
        arrayOf(
            VersionEcBlock(292, 30, 2, 116),
            VersionEcBlock(292, 22, 3, 36, 2, 37),
            VersionEcBlock(292, 20, 4, 16, 4, 17),
            VersionEcBlock(292, 24, 4, 12, 4, 13)
        ),
        // V10
        arrayOf(
            VersionEcBlock(346, 18, 2, 68, 2, 69),
            VersionEcBlock(346, 26, 4, 43, 1, 44),
            VersionEcBlock(346, 24, 6, 19, 2, 20),
            VersionEcBlock(346, 28, 6, 15, 2, 16)
        )
    )

    // Alignment pattern center positions for versions 1..10
    private val ALIGNMENT_LOCATIONS = arrayOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50)
    )

    /**
     * Encodes [text] into a QR code matrix.
     */
    fun encode(text: String, ecLevel: QrEcLevel = QrEcLevel.M): QrMatrix {
        val dataBytes = text.encodeToByteArray()
        
        // Find smallest version that fits data
        var chosenVersion = -1
        var chosenBlock: VersionEcBlock? = null
        for (v in 1..10) {
            val block = EC_TABLE[v - 1][ecLevel.ordinal]
            val charCountBits = if (v < 10) 8 else 16
            val headerBits = 4 + charCountBits // 0100 (byte mode) + length
            val totalDataBits = headerBits + dataBytes.size * 8
            val totalDataBytesNeeded = (totalDataBits + 7) / 8
            if (totalDataBytesNeeded <= block.totalDataCodewords) {
                chosenVersion = v
                chosenBlock = block
                break
            }
        }
        
        val version = if (chosenVersion != -1) chosenVersion else 10
        val block = chosenBlock ?: EC_TABLE[9][ecLevel.ordinal]
        val totalDataCodewords = block.totalDataCodewords

        // 1. Build Data Bit Buffer
        val bitBuffer = BitBuffer()
        bitBuffer.appendBits(0b0100, 4) // Byte mode
        val charCountBits = if (version < 10) 8 else 16
        bitBuffer.appendBits(dataBytes.size.coerceAtMost((1 shl charCountBits) - 1), charCountBits)
        for (b in dataBytes) {
            bitBuffer.appendBits(b.toInt() and 0xFF, 8)
        }

        // Add terminator (up to 4 zeroes)
        val maxBits = totalDataCodewords * 8
        val terminatorBits = minOf(4, maxBits - bitBuffer.length)
        bitBuffer.appendBits(0, terminatorBits)

        // Bit padding to whole byte
        while (bitBuffer.length % 8 != 0) {
            bitBuffer.appendBits(0, 1)
        }

        // Byte padding (0xEC, 0x11 alternating)
        var padToggle = true
        while (bitBuffer.length < maxBits) {
            bitBuffer.appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val allDataBytes = bitBuffer.toByteArray()

        // 2. Divide data into blocks and generate Reed-Solomon EC bytes
        val totalBlocks = block.numBlocksGroup1 + block.numBlocksGroup2
        val dataBlocks = Array(totalBlocks) { IntArray(0) }
        val ecBlocks = Array(totalBlocks) { IntArray(0) }

        var byteOffset = 0
        var blockIdx = 0
        for (i in 0 until block.numBlocksGroup1) {
            val len = block.dataCodewordsGroup1
            val slice = IntArray(len) { allDataBytes[byteOffset + it].toInt() and 0xFF }
            dataBlocks[blockIdx] = slice
            ecBlocks[blockIdx] = calculateReedSolomon(slice, block.ecCodewordsPerBlock)
            byteOffset += len
            blockIdx++
        }
        for (i in 0 until block.numBlocksGroup2) {
            val len = block.dataCodewordsGroup2
            val slice = IntArray(len) { allDataBytes[byteOffset + it].toInt() and 0xFF }
            dataBlocks[blockIdx] = slice
            ecBlocks[blockIdx] = calculateReedSolomon(slice, block.ecCodewordsPerBlock)
            byteOffset += len
            blockIdx++
        }

        // 3. Interleave data codewords then EC codewords
        val interleaved = ArrayList<Int>(block.totalCodewords)
        val maxDataLen = maxOf(block.dataCodewordsGroup1, block.dataCodewordsGroup2)
        for (col in 0 until maxDataLen) {
            for (b in 0 until totalBlocks) {
                if (col < dataBlocks[b].size) {
                    interleaved.add(dataBlocks[b][col])
                }
            }
        }
        for (col in 0 until block.ecCodewordsPerBlock) {
            for (b in 0 until totalBlocks) {
                interleaved.add(ecBlocks[b][col])
            }
        }

        // 4. Matrix Population
        val matrixSize = 17 + 4 * version
        val isFunction = Array(matrixSize) { BooleanArray(matrixSize) }
        val rawMatrix = Array(matrixSize) { BooleanArray(matrixSize) }

        // Finder Patterns & Separators
        placeFinder(rawMatrix, isFunction, 0, 0, matrixSize)
        placeFinder(rawMatrix, isFunction, 0, matrixSize - 7, matrixSize)
        placeFinder(rawMatrix, isFunction, matrixSize - 7, 0, matrixSize)

        // Alignment Patterns
        val alignLocs = ALIGNMENT_LOCATIONS[version - 1]
        for (r in alignLocs) {
            for (c in alignLocs) {
                if (!isFunction[r][c]) {
                    placeAlignment(rawMatrix, isFunction, r, c)
                }
            }
        }

        // Timing Patterns
        for (i in 8 until matrixSize - 8) {
            if (!isFunction[6][i]) {
                rawMatrix[6][i] = (i % 2 == 0)
                isFunction[6][i] = true
            }
            if (!isFunction[i][6]) {
                rawMatrix[i][6] = (i % 2 == 0)
                isFunction[i][6] = true
            }
        }

        // Dark Module
        rawMatrix[4 * version + 9][8] = true
        isFunction[4 * version + 9][8] = true

        // Reserve Format Info areas
        reserveFormatInfo(isFunction, matrixSize)

        // 5. Place Data Bits in Zigzag Order
        val dataBits = BitBuffer()
        for (b in interleaved) {
            dataBits.appendBits(b, 8)
        }
        // Remainder bits for versions 2-6 (7 bits for v2-6, 0 for v1)
        val remainderBits = getRemainderBits(version)
        repeat(remainderBits) { dataBits.appendBits(0, 1) }

        var bitIndex = 0
        var right = matrixSize - 1
        var movingUp = true

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            val cols = intArrayOf(right, right - 1)
            val rows = if (movingUp) (matrixSize - 1 downTo 0).toList() else (0 until matrixSize).toList()

            for (r in rows) {
                for (c in cols) {
                    if (!isFunction[r][c]) {
                        val bit = if (bitIndex < dataBits.length) dataBits.getBit(bitIndex++) else false
                        rawMatrix[r][c] = bit
                    }
                }
            }
            movingUp = !movingUp
            right -= 2
        }

        // 6. Evaluate 8 Mask Patterns & Select Minimum Penalty
        var bestMask = 0
        var minPenalty = Int.MAX_VALUE
        var bestMatrix = rawMatrix

        for (mask in 0..7) {
            val maskedMatrix = applyMask(rawMatrix, isFunction, matrixSize, mask)
            writeFormatInfo(maskedMatrix, matrixSize, ecLevel, mask)
            val penalty = calculatePenalty(maskedMatrix, matrixSize)
            if (penalty < minPenalty) {
                minPenalty = penalty
                bestMask = mask
                bestMatrix = maskedMatrix
            }
        }

        return QrMatrix(version, matrixSize, bestMatrix)
    }

    private fun getRemainderBits(version: Int): Int = when (version) {
        in 2..6 -> 7
        else -> 0
    }

    private fun placeFinder(matrix: Array<BooleanArray>, isFunction: Array<BooleanArray>, r: Int, c: Int, size: Int) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val row = r + dr
                val col = c + dc
                if (row in 0 until size && col in 0 until size) {
                    isFunction[row][col] = true
                    val in7x7 = dr in 0..6 && dc in 0..6
                    if (in7x7) {
                        val isBorder = dr == 0 || dr == 6 || dc == 0 || dc == 6
                        val isCenter = dr in 2..4 && dc in 2..4
                        matrix[row][col] = isBorder || isCenter
                    } else {
                        matrix[row][col] = false // Separator
                    }
                }
            }
        }
    }

    private fun placeAlignment(matrix: Array<BooleanArray>, isFunction: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val row = r + dr
                val col = c + dc
                isFunction[row][col] = true
                val isOuter = kotlin.math.abs(dr) == 2 || kotlin.math.abs(dc) == 2
                val isCenter = dr == 0 && dc == 0
                matrix[row][col] = isOuter || isCenter
            }
        }
    }

    private fun reserveFormatInfo(isFunction: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            if (i in 0 until size) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in (size - 8) until size) {
            isFunction[8][i] = true
            isFunction[i][8] = true
        }
    }

    private fun applyMask(
        raw: Array<BooleanArray>,
        isFunction: Array<BooleanArray>,
        size: Int,
        mask: Int
    ): Array<BooleanArray> {
        val result = Array(size) { r ->
            BooleanArray(size) { c ->
                val orig = raw[r][c]
                if (isFunction[r][c]) {
                    orig
                } else {
                    val invert = when (mask) {
                        0 -> (r + c) % 2 == 0
                        1 -> r % 2 == 0
                        2 -> c % 3 == 0
                        3 -> (r + c) % 3 == 0
                        4 -> ((r / 2) + (c / 3)) % 2 == 0
                        5 -> ((r * c) % 2) + ((r * c) % 3) == 0
                        6 -> (((r * c) % 2) + ((r * c) % 3)) % 2 == 0
                        7 -> (((r + c) % 2) + ((r * c) % 3)) % 2 == 0
                        else -> false
                    }
                    if (invert) !orig else orig
                }
            }
        }
        return result
    }

    private fun writeFormatInfo(matrix: Array<BooleanArray>, size: Int, ecLevel: QrEcLevel, mask: Int) {
        val data = (ecLevel.formatBits shl 3) or mask
        var rem = data shl 10
        val poly = 0x537
        for (i in 14 downTo 10) {
            if ((rem and (1 shl i)) != 0) {
                rem = rem xor (poly shl (i - 10))
            }
        }
        val formatBits = ((data shl 10) or rem) xor 0x5412

        // Helper to extract bit b (0 <= b <= 14, where b14 is MSB)
        fun getBit(b: Int): Boolean = ((formatBits shr b) and 1) != 0

        // Primary copy around top-left finder pattern:
        // Horizontal: (8,0)=b14, (8,1)=b13, (8,2)=b12, (8,3)=b11, (8,4)=b10, (8,5)=b9, (8,7)=b8, (8,8)=b7
        matrix[8][0] = getBit(14)
        matrix[8][1] = getBit(13)
        matrix[8][2] = getBit(12)
        matrix[8][3] = getBit(11)
        matrix[8][4] = getBit(10)
        matrix[8][5] = getBit(9)
        matrix[8][7] = getBit(8)
        matrix[8][8] = getBit(7)

        // Vertical: (7,8)=b6, (5,8)=b5, (4,8)=b4, (3,8)=b3, (2,8)=b2, (1,8)=b1, (0,8)=b0
        matrix[7][8] = getBit(6)
        matrix[5][8] = getBit(5)
        matrix[4][8] = getBit(4)
        matrix[3][8] = getBit(3)
        matrix[2][8] = getBit(2)
        matrix[1][8] = getBit(1)
        matrix[0][8] = getBit(0)

        // Secondary copy:
        // Bottom-left column: (size-1, 8)=b0, (size-2, 8)=b1 ... (size-7, 8)=b6
        for (i in 0..6) {
            matrix[size - 1 - i][8] = getBit(i)
        }

        // Top-right row: (8, size-8)=b7, (8, size-7)=b8 ... (8, size-1)=b14
        for (i in 0..7) {
            matrix[8][size - 8 + i] = getBit(7 + i)
        }
    }

    private fun calculatePenalty(matrix: Array<BooleanArray>, size: Int): Int {
        var penalty = 0

        // Rule 1: 5 or more same color in a row/col
        for (r in 0 until size) {
            var count = 0
            var lastColor = false
            for (c in 0 until size) {
                val color = matrix[r][c]
                if (c == 0 || color != lastColor) {
                    lastColor = color
                    count = 1
                } else {
                    count++
                    if (count == 5) penalty += 3
                    else if (count > 5) penalty += 1
                }
            }
        }
        for (c in 0 until size) {
            var count = 0
            var lastColor = false
            for (r in 0 until size) {
                val color = matrix[r][c]
                if (r == 0 || color != lastColor) {
                    lastColor = color
                    count = 1
                } else {
                    count++
                    if (count == 5) penalty += 3
                    else if (count > 5) penalty += 1
                }
            }
        }

        // Rule 2: 2x2 same color blocks
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val b = matrix[r][c]
                if (matrix[r + 1][c] == b && matrix[r][c + 1] == b && matrix[r + 1][c + 1] == b) {
                    penalty += 3
                }
            }
        }

        // Rule 4: Total dark module percentage
        var darkCount = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (matrix[r][c]) darkCount++
            }
        }
        val totalModules = size * size
        val pct = (darkCount * 100) / totalModules
        val diff5 = kotlin.math.abs(pct - 50) / 5
        penalty += diff5 * 10

        return penalty
    }

    private fun calculateReedSolomon(data: IntArray, ecCount: Int): IntArray {
        // Build generator polynomial with coefficients stored high-to-low degree:
        // gen[0] = leading coefficient (always 1), gen[ecCount] = constant term
        // g(x) = ∏(x + α^i) for i = 0..ecCount-1
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val newGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                newGen[j] = newGen[j] xor gen[j]                              // coefficient * x
                newGen[j + 1] = newGen[j + 1] xor gfMul(gen[j], expTable[i]) // coefficient * α^i
            }
            gen = newGen
        }

        // Polynomial long division: compute (data · x^ecCount) mod gen
        val result = IntArray(data.size + ecCount)
        data.copyInto(result)

        for (i in data.indices) {
            val coeff = result[i]
            if (coeff != 0) {
                for (j in 1..ecCount) {
                    result[i + j] = result[i + j] xor gfMul(gen[j], coeff)
                }
            }
        }

        // The remainder (EC codewords) occupies the last ecCount positions
        return result.copyOfRange(data.size, data.size + ecCount)
    }

    private class BitBuffer {
        private val bits = ArrayList<Boolean>()
        val length: Int get() = bits.size

        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add(((value shr i) and 1) != 0)
            }
        }

        fun getBit(index: Int): Boolean = bits[index]

        fun toByteArray(): ByteArray {
            val bytes = ByteArray((bits.size + 7) / 8)
            for (i in bits.indices) {
                if (bits[i]) {
                    bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (7 - (i % 8)))).toByte()
                }
            }
            return bytes
        }
    }
}
