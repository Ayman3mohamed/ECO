from ctypes import *
import random
from PIL import Image


def sample(probs):
    s = sum(probs)
    probs = [a / s for a in probs]
    r = random.uniform(0, 1)
    for i in range(len(probs)):
        r = r - probs[i]
        if r <= 0:
            return i
    return len(probs) - 1


def c_array(ctype, values):
    return (ctype * len(values))(*values)


class BOX(Structure):
    _fields_ = [("x", c_float),
                ("y", c_float),
                ("w", c_float),
                ("h", c_float)]


class IMAGE(Structure):
    _fields_ = [("w", c_int),
                ("h", c_int),
                ("c", c_int),
                ("data", POINTER(c_float))]


class METADATA(Structure):
    _fields_ = [("classes", c_int),
                ("names", POINTER(c_char_p))]


lib = CDLL("libdarknet.so", RTLD_GLOBAL)
lib.network_width.argtypes = [c_void_p]
lib.network_width.restype = c_int
lib.network_height.argtypes = [c_void_p]
lib.network_height.restype = c_int

predict = lib.network_predict_p
predict.argtypes = [c_void_p, POINTER(c_float)]
predict.restype = POINTER(c_float)

make_boxes = lib.make_boxes
make_boxes.argtypes = [c_void_p]
make_boxes.restype = POINTER(BOX)

free_ptrs = lib.free_ptrs
free_ptrs.argtypes = [POINTER(c_void_p), c_int]

num_boxes = lib.num_boxes
num_boxes.argtypes = [c_void_p]
num_boxes.restype = c_int

make_probs = lib.make_probs
make_probs.argtypes = [c_void_p]
make_probs.restype = POINTER(POINTER(c_float))

detect = lib.network_predict_p
detect.argtypes = [c_void_p, IMAGE, c_float, c_float, c_float, POINTER(BOX), POINTER(POINTER(c_float))]

reset_rnn = lib.reset_rnn
reset_rnn.argtypes = [c_void_p]

load_net = lib.load_network_p
load_net.argtypes = [c_char_p, c_char_p, c_int]
load_net.restype = c_void_p

free_image = lib.free_image
free_image.argtypes = [IMAGE]

letterbox_image = lib.letterbox_image
letterbox_image.argtypes = [IMAGE, c_int, c_int]
letterbox_image.restype = IMAGE

load_meta = lib.get_metadata
lib.get_metadata.argtypes = [c_char_p]
lib.get_metadata.restype = METADATA

load_image = lib.load_image_color
load_image.argtypes = [c_char_p, c_int, c_int]
load_image.restype = IMAGE

predict_image = lib.network_predict_image
predict_image.argtypes = [c_void_p, IMAGE]
predict_image.restype = POINTER(c_float)

network_detect = lib.network_detect
network_detect.argtypes = [c_void_p, IMAGE, c_float, c_float, c_float, POINTER(BOX), POINTER(POINTER(c_float))]


def classify(net, meta, im):
    out = predict_image(net, im)
    res = []
    for i in range(meta.classes):
        res.append((meta.names[i], out[i]))
    res = sorted(res, key=lambda x: -x[1])
    return res


def detect(net, meta, image, thresh=.5, hier_thresh=.5, nms=.45):
    im = load_image(image, 0, 0)
    boxes = make_boxes(net)
    probs = make_probs(net)
    num = num_boxes(net)
    network_detect(net, im, thresh, hier_thresh, nms, boxes, probs)
    res = []
    for j in range(num):
        for i in range(meta.classes):
            if probs[j][i] > 0:
                res.append((meta.names[i], probs[j][i], (boxes[j].x, boxes[j].y, boxes[j].w, boxes[j].h)))
    res = sorted(res, key=lambda x: -x[1])
    free_image(im)
    free_ptrs(cast(probs, POINTER(c_void_p)), num)
    return res


if __name__ == "__main__":
    net = load_net("cfg/densenet201.cfg", "weights/densenet201.weights", 0)
    meta = load_meta("cfg/imagenet1k.data")

    from os import listdir
    from os.path import isfile, join
    import json

    # mypath = '/mnt/drive1/ffffound_scrape/ffffound_images/'
    # mypath = '/mnt/drive1/4chan_scrape/hc/'
    # mypath = '/mnt/drive1/4chan_scrape/ic/'
    # mypath = '/mnt/drive1/4chan_scrape/g/'
    mypath = '/mnt/drive1/data/eco/gifs/clean/'
    onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]

    saved = {}

    counter = 0
    for file in onlyfiles:
        path = join(mypath, file)
        print(path)
        try:
            with Image.open(path) as img:
                split = img.split()

                if len(split) is 3:
                    im = load_image(path, 0, 0)
                    r = classify(net, meta, im)
                    if r[0] > 0.7:
                        counter += 1
                        print(str(counter) + ' / ' + str(len(onlyfiles)))
                        saved[path] = r[:10]
                        free_image(im)
                else:
                    print("rror")
                
                img.close()
        except:
            pass
    print(saved)

    # with open('test_out_ffffound.json', 'w') as fp:
    # with open('test_out_4chan_hc.json', 'w') as fp:
    # with open('test_out_4chan_ic.json', 'w') as fp:
    # with open('test_out_4chan_g.json', 'w') as fp:
    with open('test_out_4chan_clean_gifs.json', 'w') as fp:
        json.dump(saved, fp)

    net = load_net("cfg/tiny-yolo.cfg", "tiny-yolo.weights", 0)
    meta = load_meta("cfg/coco.data")
    r = detect(net, meta, "data/dog.jpg")
    r2 = classify(net, meta, load_image("data/dog.jpg", 0, 0))

    print(r)
    print(r2)


