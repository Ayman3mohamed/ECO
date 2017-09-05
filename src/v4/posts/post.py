from enum import Enum
import json
import time
import datetime
import random
import os
import socket

from posts.reddit.generator import Generator
from posts.reddit.pandasdata import PandasData
from posts.reddit.pandasfilter import PandasFilter

from posts.kaomoji.kaomoji import KaomojiHelp
from posts.image.image import ImageHelper
from posts.image.image import AsciiHelper


class Post(object):
    def __init__(self, previous, text='', user='admin'):
        self._text = text
        self._user = user
        self._attachment = None
        self._style = 'unformatted'
        self._timestamp = datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d %H:%M:%S')

        self.connection(previous)

    def connection(self, previous):
        """
        makes the connection, generates class variables depending
        on what of the previous post to take into account for
        generating the new post
        """
        raise NotImplementedError("Should have implemented this")

    def json(self):
        """
        convert a post to json in order to send it to the frontend
        """
        return json.dumps(self.msg_dict())

    def text(self):
        return self._text


    # TODO for what?
    def msg_dict(self):
        """
        returns a dict of the post
        """
        return {
            'user': self._user,
            'text': self._text,
            'attachment': self._attachment,
            'style': self._style,
            'timestamp': self._timestamp
        }


class StartPost(Post):
    """
    this post is just there for the initial post
    because all new posts require a previous post

    this is a way to define the starting topic
    """
    def __init__(self):
        super().__init__(None, text='welcome')

    def connection(self, previous=None):
        pass

# some static variable
kao = KaomojiHelp()


class KaomojiPost(Post):
    def __init__(self, previous):
        super().__init__(previous)
        self.kaomoji = kao.random()

    def connection(self, previous):
        words = previous.text().split()
        self.kaomoji = kao.find(words)
        if not self.kaomoji:
            self.kaomoji = kao.random()
        self._text = self.kaomoji.kaomoji()
        self._user = self.kaomoji.emotions()


image_helper = ImageHelper(path='data/image/ffffound_image_categories.json')
#image_helper = ImageHelper(path='data/image/test_out_4chan_hc.json')
#image_helper = ImageHelper(path='data/image/test_out_4chan_g.json')
ascii_helper = AsciiHelper()


class ImagePost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self._text = previous.text()

        if socket.gethostname() == 'lyrik':
            image_name = image_helper.find(self._text.split())
            if image_name is None:
                image_name = image_helper.random()

            image_path = os.path.join('static/image/ffffound_scrape/ffffound_images/', image_name)
            #image_path = os.path.join('static/image/4chan_hc/', image_name)
            #image_path = os.path.join('static/image/4chan_g/', image_name)
            self.path = image_path
        else:
            self.path = 'static/image/gif.gif'

        self._user = 'image'
        self._attachment = self.path

        self._ascii = bool(random.getrandbits(1))

        if self._ascii:
            self._attachment = None
            self._text = ascii_helper.image2ascii(ascii_helper.load('server/' + self.path))
            self._style = "formatted"
            if self._text is False:
                self._attachment = self.path
                self._style = "unformatted"
                self._text = ""


# some heavy, static variables
feather_file = 'data/reddit/test_reddit_4chan.feather'
block_words = open('data/reddit/blocked_words.txt').readlines()
block_chars = "".join(open('data/reddit/blocked_chars.txt').readlines())
data = PandasData(feather_file, block_words=block_words)
data.load()
df = data.df
generator = Generator(PandasFilter(df), block_words=block_words, block_chars=block_chars)


class RedditPost(Post):
    def __init__(self, previous):
        """
        uses a class that generates(filters) a new post from reddit/4chan
        this class can be more detailed parametrized. check Generator
        :param previous:
        """
        super().__init__(previous)

    def connection(self, previous):
        generator.reset()
        generator.clear()
        generator.length(150, 700).shannon_entropy(1.0, 10)
        generator.generate()
        self._text = generator.sentences()[0].text
        self._user = 'reddit'


class AsciiPost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self.butterfly = open('data/ascii/butterfly.txt', 'r').readlines()
        self._text = ""
        for line in self.butterfly:
            self._text += line
        self._user = 'ascii'
        self._style = 'formatted'


class EmojiPost(Post):
    def __init__(self, previous):
        super().__init__(previous)

    def connection(self, previous):
        self._text = 'i like :smiley:'
        self._user = "emo"
