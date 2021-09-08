import h2o
import os

class ModelSerializer(object):

    def _serializer_load(self, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            h2o.init()
            return h2o.load_model(object_file_path)
        else:
            return super(ModelSerializer, self)._serializer_load(object_file_path)

    def _serializer_dump(self, obj, object_file_path):
        if object_file_path.split(os.sep)[-1] == 'model':
            object_file_path = object_file_path[:-6]
            h2o.save_model(model=obj.leader, path=object_file_path, force=True)
            os.rename(object_file_path + '/' + obj.leader.model_id, object_file_path + '/model')
        else:
            super(ModelSerializer, self)._serializer_dump(obj, object_file_path)
