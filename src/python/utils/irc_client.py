import pydle
import argparse
import sys
import os
import subprocess
import shlex

class EcoStatistics(pydle.Client):
    def on_connect(self):
        super().on_connect()
        self.join('#eco')
        self.markov_used = 0
        self.original_used = 0

    def on_private_message(self, by, message):
        super().on_private_message(by, message)
        if 'markov' in message:
            self.markov_used += 1
        elif 'original' in message:
            self.original_used += 1
        elif '--statistic' in message:
            answer = 'Markov: ' + str(self.markov_used) + ' Original: ' + str(self.original_used)
            self.message(by, answer)


def process_arguments(args):
    parser = argparse.ArgumentParser(description='configure the irc clients')

    parser.add_argument('--txts_path', action='store', help='path to folder with txt files')
    parser.add_argument('--max_bots', action='store', help='the maximum number of bots to train and connect to IRC')
    parser.add_argument('--server', action='store', help='the server to connect the bots to')

    params = vars(parser.parse_args(args))

    return params


if __name__ == '__main__':
    params = process_arguments(sys.argv[1:])
    txts_path = params['txts_path']
    max_bots = int(params['max_bots'])
    server = params['server']

    count = 0
    processes = []
    authors = []
    filename = []
    for file in os.listdir(txts_path):
        if not file.endswith('.txt') or count >= max_bots:
            continue

        path = txts_path + file

        f = open(path, 'r')
        print('Loaded ' + path)
        line_count = 0
        lines = []
        for line in f:
            line_count += 1
            lines.append(line)

        if line_count < 100:
            continue
        # call python externally here
        line = '/home/mar/.virtualenvs/eco3/bin/python /home/mar/code/marcel/ECO/src/python/utils/irc_bot.py --txt_path /home/mar/code/marcel/ECO/src/python/utils/test_pdfs_txt/ --file_name ' + file + ' --server ' + server
        subprocess.Popen(shlex.split(line), shell=False)
        #os.system(line)
        #author = file.partition('-')[0]
        #author = author[:15]
        #author.replace('.', '')
        #if author in authors:
        #    author += '_'
        #authors.append(author)

        #p = MarkovCalculator(lines, author, filename=file)
        #p.start()
        #processes.append(p)

        count += 1

    #for p in processes:
    #    p.join()

    #pool = pydle.ClientPool()
    #for p in processes:
    #    client = EcoIrcClient(p.markov_chain.prefix)
    #    client.set_markov(p.markov_chain.prefix, p.markov_chain, p.filename)
    #    client.connect(server, tls=False)
    #    pool.add(client)

    statistic_client = EcoStatistics('STATISTIC_BOT')
    statistic_client.connect(server, tls=False)
    statistic_client.handle_forever()
    #pool.add(statistic_client)
    print('Setup done.')
    #pool.handle_forever()
