package com.toanlv.flashsale.common.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class LeaderLockTest {

  @Mock RedissonClient redisson;
  @Mock RLock rLock;

  private LeaderLock leaderLock;

  @BeforeEach
  void setUp() {
    leaderLock = new LeaderLock(redisson);
    when(redisson.getLock(anyString())).thenReturn(rLock);
  }

  @Test
  void tryAcquire_returnsTrue_whenLockAcquired() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenReturn(true);

    assertThat(leaderLock.tryAcquire("test", 30)).isTrue();
  }

  @Test
  void tryAcquire_returnsFalse_whenLockNotAcquired() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenReturn(false);

    assertThat(leaderLock.tryAcquire("test", 30)).isFalse();
  }

  @Test
  void tryAcquire_returnsFalse_whenInterrupted() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

    assertThat(leaderLock.tryAcquire("test", 30)).isFalse();
    assertThat(Thread.currentThread().isInterrupted()).isTrue();

    // Clear interrupted flag
    Thread.interrupted();
  }

  @Test
  void release_unlocksWhenHeldByCurrentThread() {
    when(rLock.isHeldByCurrentThread()).thenReturn(true);

    leaderLock.release("test");

    verify(rLock).unlock();
  }

  @Test
  void release_noOp_whenNotHeldByCurrentThread() {
    when(rLock.isHeldByCurrentThread()).thenReturn(false);

    leaderLock.release("test");

    verify(rLock, never()).unlock();
  }

  @Test
  void runIfLeader_executesTask_whenLockAcquired() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenReturn(true);
    when(rLock.isHeldByCurrentThread()).thenReturn(true);

    var executed = new AtomicBoolean(false);
    var result = leaderLock.runIfLeader("test", 30, () -> executed.set(true));

    assertThat(result).isTrue();
    assertThat(executed.get()).isTrue();
    verify(rLock).unlock();
  }

  @Test
  void runIfLeader_skipsTask_whenLockNotAcquired() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenReturn(false);

    var executed = new AtomicBoolean(false);
    var result = leaderLock.runIfLeader("test", 30, () -> executed.set(true));

    assertThat(result).isFalse();
    assertThat(executed.get()).isFalse();
  }

  @Test
  void runIfLeader_releasesLock_evenIfTaskThrows() throws InterruptedException {
    when(rLock.tryLock(0, 30L, TimeUnit.SECONDS)).thenReturn(true);
    when(rLock.isHeldByCurrentThread()).thenReturn(true);

    try {
      leaderLock.runIfLeader(
          "test",
          30,
          () -> {
            throw new RuntimeException("task failed");
          });
    } catch (RuntimeException ignored) {
    }

    verify(rLock).unlock();
  }

  @Test
  void usesLockPrefixInKey() {
    leaderLock.tryAcquire("outbox-dispatcher", 30);

    verify(redisson).getLock("leader:lock:outbox-dispatcher");
  }
}
