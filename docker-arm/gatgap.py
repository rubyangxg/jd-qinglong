#!/usr/bin/python3
import sys
import cv2

image_target_path = sys.argv[1]
image_template_path = sys.argv[2]
img = cv2.imread(image_target_path, 0)
img2 = img.copy()
template = cv2.imread(image_template_path, 0)
w, h = template.shape[::-1]
methods = ['cv2.TM_CCOEFF', 'cv2.TM_CCORR']
for meth in methods:
    img = img2.copy()
    method = eval(meth)
    res = cv2.matchTemplate(img, template, method)
    min_val, max_val, min_loc, max_loc = cv2.minMaxLoc(res)
    top_left = max_loc
    bottom_right = (top_left[0] + w, top_left[1] + h)
    cv2.rectangle(img, top_left, bottom_right, 255, 2)
    print(top_left[0])
