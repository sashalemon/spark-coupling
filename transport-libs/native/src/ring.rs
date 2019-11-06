//! Contains the shared memory transport for libcourier
use memmap::{Mmap, Protection, MmapView};
use std::io::Write;
use std::ffi::CString;
use libc::{sem_open,sem_close,sem_wait,sem_post,sem_t, O_RDWR, O_CREAT, S_IRWXU};

/// `Ring` is a mmap-based ring buffer designed to transfer row-oriented data.
/// It lives in shared memory and relies on posix semaphores to control access.
/// Thus, `Ring` will only work on POSIX-compliant systems.
#[derive(Debug)]
pub struct Ring {
    views: Vec<MmapView>,
    row_size: usize,
    rows_per_block: usize,
    current: usize,
    written: usize,
    empty: *mut sem_t,
    full: *mut sem_t,
    done: *mut sem_t,
}

fn split_view(n: usize, view: MmapView) -> Vec<MmapView> {
    let length = view.len();
    let (v1, v2) = view.split_at(length / 2).unwrap();
    let n1 = n / 2;
    if n1 == 1 {
        vec![v1, v2]
    } else {
        let mut result = split_view(n1, v1);
        result.extend(split_view(n1, v2));
        result
    }
}

impl Ring {
    /// Creates a new ring buffer.
    ///
    /// * Needs to know the `row_size` of rows in the data it will transfer.
    /// * `rows_per_block` and `nblocks` are tuning parameters
    /// * `rows_per_block` specifies how many rows the buffer will write before handing control to a reader.
    /// * `nblocks` controls how many chunks of size `rows_per_block` the buffer will
    /// reserve in shared memory.
    pub fn new(row_size: usize, rows_per_block: usize, nblocks: usize) -> Ring {
        let length = row_size * nblocks * rows_per_block;
        let flags = O_RDWR | O_CREAT;
        let (full, empty, done) = unsafe {
            (sem_open(CString::new("/packfull").unwrap().as_ptr(), flags, S_IRWXU, 0), 
             sem_open(CString::new("/packempty").unwrap().as_ptr(), flags, S_IRWXU, nblocks - 1),
             sem_open(CString::new("/packack").unwrap().as_ptr(), flags, S_IRWXU, 0))
        };
        let file = ::std::fs::OpenOptions::new()
            .read(true)
            .write(true)
            .create(true)
            .open("/dev/shm/packmule").unwrap();
        file.set_len(length as u64).unwrap();

        let buf = Mmap::open_with_offset(&file, Protection::ReadWrite, 
                                         0, length).unwrap();
        let base_view = buf.into_view();
        let views = split_view(nblocks, base_view);
        Ring {
            views: views, 
            row_size: row_size, 
            rows_per_block: rows_per_block, 
            current: 0, 
            written: 0, 
            empty: empty, 
            full: full,
            done: done,
        }
    }

    /// Needs to be called after `write_all`.
    /// Flushes the last writes to the client,
    /// and waits for the client to acknowledge.
    /// If Ring actually overrode `write_all` instead of just `write`,
    /// this could be merged into that function.
    pub fn finalize(&mut self) {
        self.flush().unwrap();
        unsafe { 
            sem_post(self.full); 
            sem_wait(self.done);
        }
    }

    fn rotate_buf(&mut self) -> usize {
        self.flush().unwrap();
        unsafe {
            sem_post(self.full);
            sem_wait(self.empty);
        }
        self.current = (self.current + 1) % self.views.len();
        self.written = 0;
        self.views[self.current].len()
    }
}

impl ::std::ops::Drop for Ring {
    fn drop(&mut self) {
        unsafe {
            //sem_unlink(...); make the other side do this, and ensure they always open second?
            //sem_unlink(...; or save the names
            sem_close(self.empty);
            sem_close(self.full);
            sem_close(self.done);
        }
        ::std::fs::remove_file("/dev/shm/packmule").unwrap();
    }
}

impl ::std::io::Write for Ring {
    fn write(&mut self, buf: &[u8]) -> ::std::io::Result<usize> {
        let space_remaining = match self.views[self.current].len() - self.written {
            0 => self.rotate_buf(),
            n => n
        };
        let ref mut out = unsafe {self.views[self.current].as_mut_slice()};
        let to_write = ::std::cmp::min(space_remaining, buf.len());
        let offset = self.written;
        self.written += to_write;
        //println!("{}", to_write);
        //println!("{:?}", buf);
        out[offset..(offset + to_write)].copy_from_slice(&buf[0..to_write]);
        Ok(to_write)
    }
    fn flush(&mut self) -> ::std::io::Result<()> {
        self.views[self.current].flush()
    }
}

#[cfg(test)]
mod tests {
    use std::path::Path;
    use Ring;
    #[test]
    fn it_works() {
        {
            let _ = Ring::new();
        }
        assert!(!Path::new("/dev/shm/packmule").exists());
    }
}
