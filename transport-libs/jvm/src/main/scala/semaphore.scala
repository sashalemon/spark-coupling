package edu.byu.csl.courier.posix.semaphore

import jnr.ffi.{LibraryLoader, Pointer};
import jnr.constants.platform.linux.OpenFlags;

private trait LibC {
  def puts(data: String): Int
  def sem_open(name: String, oflag: Int): Pointer//, mode: Int, value: Int): Pointer
  def sem_wait(sem: Pointer): Int
  def sem_post(sem: Pointer): Int
  def sem_close(sem: Pointer): Int
  def sem_unlink(name: String): Int
}

private object Libs {
  val libc = LibraryLoader.create(classOf[LibC]).load("pthread")
}
import Libs._

class Semaphore(name: String) {
  val sem = libc.sem_open(name, OpenFlags.O_RDWR.value | OpenFlags.O_CREAT.value) 
  libc.sem_unlink(name)
  def _post() {
    libc.sem_post(sem)
  }
  def _wait() {
    libc.sem_wait(sem)
  }
  def close() {
    libc.sem_close(sem)
  }
}
