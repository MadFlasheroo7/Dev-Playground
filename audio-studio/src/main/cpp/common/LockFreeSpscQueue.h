#pragma once

#include <atomic>
#include <cstddef>
#include <type_traits>

template <typename T, size_t Capacity>
class LockFreeSpscQueue {
    static_assert((Capacity & (Capacity - 1)) == 0, "Capacity must be a power of 2");
    static_assert(std::is_trivially_copyable_v<T>, "T must be trivially copyable");

public:
    bool tryPush(const T& item) noexcept {
        const size_t currentWrite = mWriteIndex.load(std::memory_order_relaxed);
        const size_t nextWrite = (currentWrite + 1) & kMask;

        if (nextWrite == mReadIndex.load(std::memory_order_acquire)) {
            return false; // Queue is full
        }

        mBuffer[currentWrite] = item;
        mWriteIndex.store(nextWrite, std::memory_order_release);
        return true;
    }

    bool tryPop(T& outItem) noexcept {
        const size_t currentRead = mReadIndex.load(std::memory_order_relaxed);

        if (currentRead == mWriteIndex.load(std::memory_order_acquire)) {
            return false; // Queue is empty
        }

        outItem = mBuffer[currentRead];
        mReadIndex.store((currentRead + 1) & kMask, std::memory_order_release);
        return true;
    }

    size_t availableToRead() const noexcept {
        const size_t w = mWriteIndex.load(std::memory_order_acquire);
        const size_t r = mReadIndex.load(std::memory_order_relaxed);
        return (w - r) & kMask;
    }

private:
    static constexpr size_t kMask = Capacity - 1;

    // Align indices to 64-byte cache lines to eliminate false sharing across CPU cores
    alignas(64) std::atomic<size_t> mWriteIndex{0};
    alignas(64) std::atomic<size_t> mReadIndex{0};
    alignas(64) T mBuffer[Capacity];
};
