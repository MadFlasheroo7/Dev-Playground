#pragma once
#include <linux/uinput.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>
#include <iostream>

class VirtualMouse {
private:
    int fd = -1;

public:
    VirtualMouse() {
        fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
        if (fd < 0) {
            std::cerr << "[VirtualMouse] Warning: Could not open /dev/uinput\n";
            return;
        }

        ioctl(fd, UI_SET_EVBIT, EV_REL);
        ioctl(fd, UI_SET_RELBIT, REL_WHEEL);
        ioctl(fd, UI_SET_RELBIT, REL_WHEEL_HI_RES);

        struct uinput_setup usetup;
        memset(&usetup, 0, sizeof(usetup));
        usetup.id.bustype = BUS_USB;
        usetup.id.vendor = 0x1234;
        usetup.id.product = 0x5678;
        strcpy(usetup.name, "DevControls Virtual Air Mouse");

        if (ioctl(fd, UI_DEV_SETUP, &usetup) < 0 || ioctl(fd, UI_DEV_CREATE) < 0) {
            std::cerr << "[VirtualMouse] Failed to create virtual uinput device\n";
            close(fd);
            fd = -1;
            return;
        }
        std::cout << "[VirtualMouse] Air Mouse initialized for native pinch scrolling (REL_WHEEL + HI_RES 120).\n";
    }

    ~VirtualMouse() {
        if (fd >= 0) {
            ioctl(fd, UI_DEV_DESTROY);
            close(fd);
            fd = -1;
        }
    }

    bool is_ready() const { return fd >= 0; }

    void scroll(int steps) {
        if (fd < 0 || steps == 0) return;

        // Modern Linux/Wayland libinput expects BOTH REL_WHEEL and REL_WHEEL_HI_RES (120 units per click)
        struct input_event ev[3];
        memset(ev, 0, sizeof(ev));

        ev[0].type = EV_REL;
        ev[0].code = REL_WHEEL;
        ev[0].value = steps; // positive = up, negative = down

        ev[1].type = EV_REL;
        ev[1].code = REL_WHEEL_HI_RES;
        ev[1].value = steps * 120; // High-resolution scroll tick

        ev[2].type = EV_SYN;
        ev[2].code = SYN_REPORT;
        ev[2].value = 0;

        ssize_t written = write(fd, ev, sizeof(ev));
        (void)written;
    }
};
