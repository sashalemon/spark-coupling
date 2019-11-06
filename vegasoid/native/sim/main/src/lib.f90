module splineval
    use iso_c_binding
    use bspline_module
    use bspline_kinds_module, only: wp
    implicit none
    private
    integer,parameter :: order=4, derivative_no = 0
    type(bspline_1d) :: s1

    contains
        subroutine splineval_init(nx, x, fcn_1d) bind(C)
            integer,intent(in) :: nx
            real(wp),intent(in) :: x(nx), fcn_1d(nx)
            integer :: i
            s1 = bspline_1d(x,fcn_1d,order); if (.not. s1%status_ok()) error stop 'error initializing s1'
        end subroutine splineval_init

        subroutine classify(nx, x, y, c) bind(C)
            integer,intent(in) :: nx
            real(wp),intent(in) :: x(nx), y(nx)
            integer,intent(out) :: c(nx)
            real(wp) :: s1val
            integer :: i, iflag
            do i = 1, size(x)
                call s1%evaluate(x(i),derivative_no,s1val,iflag)
                c(i) = merge(1, -1, s1val > y(i)) ! 1,-1 to cancel out if summed
            end do
        end subroutine classify

        subroutine throw_darts(ndarts, nx, x, y, c) bind(C)
            !integer, parameter :: ndarts = 100
            integer,intent(in) :: ndarts, nx
            real(wp),intent(in) :: x(ndarts), y(ndarts)
            integer,intent(out) :: c(ndarts)
            integer :: i, iflag
            real(wp) :: sx(ndarts), sy(ndarts)

            ! Throw some test darts
            !call random_number(x)
            !call random_number(y)
            sx = x * nx !(nx - 1) + 1
            sy = y * nx !(2*nx) - nx
            call classify(nx, x, y, c)
        end subroutine throw_darts

        ! Points to plot the spline
        subroutine spline_dump(nx) bind(C)
            integer, parameter :: ndarts = 100
            integer,intent(in) :: nx
            real(wp) :: val, tx(nx*100), ty(nx*100), dartsx(ndarts), dartsy(ndarts)
            integer :: i, iflag, hits(ndarts), ntx
            ntx = nx*100

            tx = linspace(1._wp,dble(nx),ntx)

            ! Plot the interpolated line
            do i=1,ntx
                call s1%evaluate(tx(i),derivative_no,ty(i),iflag)
            end do

            ! Throw some test darts
            call throw_darts(ndarts, nx, dartsx, dartsy, hits)
            !call random_number(dartsx)
            !call random_number(dartsy)
            !dartsx = dartsx * (nx - 1) + 1
            !dartsy = dartsy * (2*nx) - nx
            !call classify(nx, dartsx, dartsy, hits)

            ! Write results
            open(9, file='interp.dat', form="unformatted", access='stream')
            write(9) ntx, tx, ty
            close(9)
            open(10, file='darts.dat', form="unformatted", access='stream')
            write(10) ndarts, dartsx, dartsy, hits
            close(10)
        end subroutine spline_dump

        ! Helpful translation of linspace from:
        ! http://www.math.unm.edu/~motamed/Teaching/F17/HPSC/fortran.html#more-on-arrays
        pure function linspace(a,b,n)
            implicit none
            real(wp),intent(in) :: a,b
            integer,intent(in) :: n
            real(wp) :: linspace(n)
            integer :: i
            linspace = a+(b-a)/(n-1)*[(i, i=0, n-1)]
        end function linspace
end module splineval
